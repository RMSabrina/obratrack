package com.unaj.appmoviles.obratrack.core.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.unaj.appmoviles.obratrack.core.domain.model.GravedadIncidente
import com.unaj.appmoviles.obratrack.core.domain.model.Incidente
import com.unaj.appmoviles.obratrack.core.domain.repository.IncidenteRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IncidenteRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : IncidenteRepository {

    private val localIncidentes = MutableStateFlow<Map<String, List<Incidente>>>(emptyMap())

    override fun observeIncidentes(obraId: String): Flow<List<Incidente>> = callbackFlow {
        val collection = firestore.collection("obras").document(obraId).collection("incidentes")
        val listener = collection.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) {
                trySend(localIncidentes.value[obraId].orEmpty())
                return@addSnapshotListener
            }

            val remotos = snapshot.documents.mapNotNull { doc ->
                val titulo = doc.getString("titulo") ?: return@mapNotNull null
                val descripcion = doc.getString("descripcion") ?: ""
                val gravedadStr = doc.getString("gravedad") ?: GravedadIncidente.MEDIA.name
                val gravedad = try { GravedadIncidente.valueOf(gravedadStr) } catch (_: Exception) { GravedadIncidente.MEDIA }
                val reportadoPorUserId = doc.getString("reportadoPorUserId") ?: ""
                val reportadoPorNombre = doc.getString("reportadoPorNombre") ?: "Inspector"
                val fecha = doc.getLong("fecha") ?: System.currentTimeMillis()
                val resuelto = doc.getBoolean("resuelto") ?: false
                val fotoUrl = doc.getString("fotoUrl")

                Incidente(
                    id = doc.id,
                    obraId = obraId,
                    titulo = titulo,
                    descripcion = descripcion,
                    gravedad = gravedad,
                    reportadoPorUserId = reportadoPorUserId,
                    reportadoPorNombre = reportadoPorNombre,
                    fecha = fecha,
                    resuelto = resuelto,
                    fotoUrl = fotoUrl
                )
            }.sortedByDescending { it.fecha }

            val combinados = (remotos + localIncidentes.value[obraId].orEmpty())
                .distinctBy { it.id }
                .sortedByDescending { it.fecha }

            trySend(combinados)
        }

        awaitClose { listener.remove() }
    }

    override suspend fun reportarIncidente(incidente: Incidente): Result<Unit> = runCatching {
        val id = if (incidente.id.isBlank()) UUID.randomUUID().toString() else incidente.id
        val item = incidente.copy(id = id)

        val actuales = localIncidentes.value[item.obraId].orEmpty()
        localIncidentes.value = localIncidentes.value + (item.obraId to (listOf(item) + actuales))

        try {
            val map = hashMapOf(
                "titulo" to item.titulo,
                "descripcion" to item.descripcion,
                "gravedad" to item.gravedad.name,
                "reportadoPorUserId" to item.reportadoPorUserId,
                "reportadoPorNombre" to item.reportadoPorNombre,
                "fecha" to item.fecha,
                "resuelto" to item.resuelto,
                "fotoUrl" to item.fotoUrl
            )
            firestore.collection("obras")
                .document(item.obraId)
                .collection("incidentes")
                .document(id)
                .set(map)
                .await()
        } catch (_: Exception) {
            // Guardado en cache reactiva local
        }
    }

    override suspend fun resolverIncidente(obraId: String, incidenteId: String): Result<Unit> = runCatching {
        val actuales = localIncidentes.value[obraId].orEmpty()
        localIncidentes.value = localIncidentes.value + (obraId to actuales.map {
            if (it.id == incidenteId) it.copy(resuelto = true) else it
        })

        try {
            firestore.collection("obras")
                .document(obraId)
                .collection("incidentes")
                .document(incidenteId)
                .update("resuelto", true)
                .await()
        } catch (_: Exception) {
            // Silencioso
        }
    }
}
