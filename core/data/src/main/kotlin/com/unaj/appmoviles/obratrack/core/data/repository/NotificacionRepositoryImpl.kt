package com.unaj.appmoviles.obratrack.core.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.unaj.appmoviles.obratrack.core.domain.model.NotificacionItem
import com.unaj.appmoviles.obratrack.core.domain.model.TipoNotificacion
import com.unaj.appmoviles.obratrack.core.domain.repository.NotificacionRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificacionRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : NotificacionRepository {

    // Fallback reactive cache with demo seed for immediate testability
    private val localNotifications = MutableStateFlow<List<NotificacionItem>>(emptyList())
    private var initializedDemo = false

    private fun ensureSeedNotifications(userId: String) {
        if (!initializedDemo) {
            val now = System.currentTimeMillis()
            val dayMillis = 24L * 60 * 60 * 1000L
            localNotifications.value = listOf(
                NotificacionItem(
                    id = "demo-1",
                    userId = userId,
                    titulo = "🔔 Visita técnica programada",
                    mensaje = "Tenés inspección técnica programada para hoy en 'Edificio Mitre'.",
                    tipo = TipoNotificacion.VISITA_RECORDATORIO,
                    fecha = now - 30 * 60 * 1000L,
                    leida = false,
                    rutaDestino = "agenda"
                ),
                NotificacionItem(
                    id = "demo-2",
                    userId = userId,
                    titulo = "⚠️ Alerta de Inactividad",
                    mensaje = "La obra 'Torre Alvear' lleva 7 días sin nuevos relevamientos registrados.",
                    tipo = TipoNotificacion.OBRA_INACTIVA,
                    fecha = now - dayMillis,
                    leida = false,
                    rutaDestino = "obras"
                ),
                NotificacionItem(
                    id = "demo-3",
                    userId = userId,
                    titulo = "🚨 Incidente Reportado",
                    mensaje = "El inspector técnico reportó una rotura en encofrado del 2do piso.",
                    tipo = TipoNotificacion.INCIDENTE_REPORTADO,
                    fecha = now - 2 * dayMillis,
                    leida = true,
                    rutaDestino = "obras"
                ),
                NotificacionItem(
                    id = "demo-4",
                    userId = userId,
                    titulo = "📸 Novedad Publicada",
                    mensaje = "Se agregaron 4 nuevas fotos al avance semanal para supervisión del cliente.",
                    tipo = TipoNotificacion.NOVEDAD_PUBLICADA,
                    fecha = now - 3 * dayMillis,
                    leida = true,
                    rutaDestino = "obras"
                )
            )
            initializedDemo = true
        }
    }

    override fun observe(userId: String): Flow<List<NotificacionItem>> = callbackFlow {
        ensureSeedNotifications(userId)

        // Escucha en tiempo real de Firestore
        val collection = firestore.collection("users").document(userId).collection("notificaciones")
        val listenerRegistration = collection.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null || snapshot.isEmpty) {
                // Si la colección remota está vacía o da error de permisos en demo, usamos la lista local reactiva
                trySend(localNotifications.value)
                return@addSnapshotListener
            }

            val remoteItems = snapshot.documents.mapNotNull { doc ->
                val titulo = doc.getString("titulo") ?: return@mapNotNull null
                val mensaje = doc.getString("mensaje") ?: ""
                val tipoStr = doc.getString("tipo") ?: TipoNotificacion.VISITA_RECORDATORIO.name
                val tipo = try {
                    TipoNotificacion.valueOf(tipoStr)
                } catch (_: Exception) {
                    TipoNotificacion.VISITA_RECORDATORIO
                }
                val fecha = doc.getLong("fecha") ?: System.currentTimeMillis()
                val leida = doc.getBoolean("leida") ?: false
                val obraId = doc.getString("obraId")
                val rutaDestino = doc.getString("rutaDestino")

                NotificacionItem(
                    id = doc.id,
                    userId = userId,
                    obraId = obraId,
                    titulo = titulo,
                    mensaje = mensaje,
                    tipo = tipo,
                    fecha = fecha,
                    leida = leida,
                    rutaDestino = rutaDestino
                )
            }.sortedByDescending { it.fecha }

            val combined = (remoteItems + localNotifications.value)
                .distinctBy { it.id }
                .sortedByDescending { it.fecha }

            trySend(combined)
        }

        awaitClose {
            listenerRegistration.remove()
        }
    }

    override suspend fun marcarComoLeida(id: String): Result<Unit> = runCatching {
        // Actualizar local
        localNotifications.value = localNotifications.value.map {
            if (it.id == id) it.copy(leida = true) else it
        }
        // Intentar actualizar en Firestore si existe
        try {
            val user = firestore.collectionGroup("notificaciones")
                .whereEqualTo("id", id)
                .get()
                .await()
            for (doc in user.documents) {
                doc.reference.update("leida", true).await()
            }
        } catch (_: Exception) {
            // Ignorado si no hay conexión o no está en la nube
        }
    }

    override suspend fun marcarTodasComoLeidas(userId: String): Result<Unit> = runCatching {
        localNotifications.value = localNotifications.value.map { it.copy(leida = true) }
        try {
            val unread = firestore.collection("users")
                .document(userId)
                .collection("notificaciones")
                .whereEqualTo("leida", false)
                .get()
                .await()
            for (doc in unread.documents) {
                doc.reference.update("leida", true).await()
            }
        } catch (_: Exception) {
            // Silencioso en modo offline
        }
    }

    override suspend fun crearNotificacion(notificacion: NotificacionItem): Result<Unit> = runCatching {
        val item = if (notificacion.id.isBlank()) {
            notificacion.copy(id = UUID.randomUUID().toString())
        } else {
            notificacion
        }
        localNotifications.value = listOf(item) + localNotifications.value
        try {
            val map = hashMapOf(
                "titulo" to item.titulo,
                "mensaje" to item.mensaje,
                "tipo" to item.tipo.name,
                "fecha" to item.fecha,
                "leida" to item.leida,
                "obraId" to item.obraId,
                "rutaDestino" to item.rutaDestino
            )
            firestore.collection("users")
                .document(item.userId)
                .collection("notificaciones")
                .document(item.id)
                .set(map)
                .await()
        } catch (_: Exception) {
            // Se mantiene en el cache local
        }
    }

    override suspend fun eliminarNotificacion(id: String): Result<Unit> = runCatching {
        localNotifications.value = localNotifications.value.filterNot { it.id == id }
    }
}
