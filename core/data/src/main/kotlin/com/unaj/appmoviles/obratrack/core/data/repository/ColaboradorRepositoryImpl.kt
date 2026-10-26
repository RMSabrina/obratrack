package com.unaj.appmoviles.obratrack.core.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.unaj.appmoviles.obratrack.core.domain.model.Colaborador
import com.unaj.appmoviles.obratrack.core.domain.model.UserRole
import com.unaj.appmoviles.obratrack.core.domain.repository.ColaboradorRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ColaboradorRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : ColaboradorRepository {

    private val localColaboradores = MutableStateFlow<Map<String, List<Colaborador>>>(emptyMap())

    override fun observeColaboradores(obraId: String): Flow<List<Colaborador>> = callbackFlow {
        val collection = firestore.collection("obras").document(obraId).collection("colaboradores")
        val listener = collection.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) {
                trySend(localColaboradores.value[obraId].orEmpty())
                return@addSnapshotListener
            }

            val remotos = snapshot.documents.mapNotNull { doc ->
                val email = doc.getString("email") ?: return@mapNotNull null
                val nombre = doc.getString("nombre") ?: email.substringBefore("@")
                val rolStr = doc.getString("rol") ?: UserRole.INSPECTOR.name
                val rol = try { UserRole.valueOf(rolStr) } catch (_: Exception) { UserRole.INSPECTOR }
                val fecha = doc.getLong("fechaAsignacion") ?: System.currentTimeMillis()

                Colaborador(
                    id = doc.id,
                    obraId = obraId,
                    userId = doc.getString("userId") ?: "",
                    email = email,
                    nombre = nombre,
                    rol = rol,
                    fechaAsignacion = fecha
                )
            }

            val combinados = (remotos + localColaboradores.value[obraId].orEmpty()).distinctBy { it.id }
            trySend(combinados)
        }

        awaitClose { listener.remove() }
    }

    override suspend fun agregarColaborador(
        obraId: String,
        email: String,
        nombre: String,
        rol: UserRole
    ): Result<Colaborador> = runCatching {
        val id = UUID.randomUUID().toString()
        val nuevo = Colaborador(
            id = id,
            obraId = obraId,
            userId = "",
            email = email.trim(),
            nombre = nombre.trim().ifBlank { email.substringBefore("@") },
            rol = rol,
            fechaAsignacion = System.currentTimeMillis()
        )

        val actuales = localColaboradores.value[obraId].orEmpty()
        localColaboradores.value = localColaboradores.value + (obraId to (actuales + nuevo))

        try {
            val map = hashMapOf(
                "email" to nuevo.email,
                "nombre" to nuevo.nombre,
                "rol" to nuevo.rol.name,
                "fechaAsignacion" to nuevo.fechaAsignacion
            )
            firestore.collection("obras")
                .document(obraId)
                .collection("colaboradores")
                .document(id)
                .set(map)
                .await()
        } catch (_: Exception) {
            // Guardado en cache reactiva
        }

        nuevo
    }

    override suspend fun removerColaborador(obraId: String, colaboradorId: String): Result<Unit> = runCatching {
        val actuales = localColaboradores.value[obraId].orEmpty()
        localColaboradores.value = localColaboradores.value + (obraId to actuales.filterNot { it.id == colaboradorId })

        try {
            firestore.collection("obras")
                .document(obraId)
                .collection("colaboradores")
                .document(colaboradorId)
                .delete()
                .await()
        } catch (_: Exception) {
            // Silencioso
        }
    }
}
