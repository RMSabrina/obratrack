package com.unaj.appmoviles.obratrack.core.firebase

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date
import kotlin.coroutines.cancellation.CancellationException

class FirestoreRelevamientoDataSourceImpl(
    private val firestore: FirebaseFirestore
) : FirestoreRelevamientoDataSource {

    override fun observe(userId: String, obraId: String): Flow<List<RelevamientoDto>> = callbackFlow {
        val registration = relevamientos(obraId)
            .whereEqualTo(FIELD_USER_ID, userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val items = snapshot?.documents?.mapNotNull { it.toRelevamientoDto(obraId) }.orEmpty()
                trySend(items)
            }
        awaitClose { registration.remove() }
    }

    override suspend fun create(dto: RelevamientoDto): FirebaseResult<RelevamientoDto> {
        return try {
            val obraRef = firestore.collection(OBRAS).document(dto.obraId)
            val obra = obraRef.get().await()
            if (!obra.exists()) return FirebaseResult.Error(ObraNotFoundException())
            if (obra.getString(FIELD_USER_ID) != dto.userId) {
                return FirebaseResult.Error(ObraForbiddenException())
            }

            val relevamientoRef = relevamientos(dto.obraId).document()
            val stored = dto.copy(id = relevamientoRef.id)
            val currentUltimaVisita = obra.getTimestamp(FIELD_ULTIMA_VISITA)?.toDate()?.time
            val ultimaVisita = maxOf(stored.fechaMillis, currentUltimaVisita ?: stored.fechaMillis)

            firestore.batch().apply {
                set(relevamientoRef, stored.toMap())
                update(obraRef, FIELD_ULTIMA_VISITA, Timestamp(Date(ultimaVisita)))
            }.commit().await()

            FirebaseResult.Success(stored)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FirebaseResult.Error(e)
        }
    }

    override suspend fun update(userId: String, dto: RelevamientoDto): FirebaseResult<RelevamientoDto> {
        return try {
            val obraRef = firestore.collection(OBRAS).document(dto.obraId)
            val obra = obraRef.get().await()
            if (!obra.exists()) return FirebaseResult.Error(ObraNotFoundException())
            if (obra.getString(FIELD_USER_ID) != userId) {
                return FirebaseResult.Error(ObraForbiddenException())
            }

            val ref = relevamientos(dto.obraId).document(dto.id)
            val snapshot = ref.get().await()
            if (!snapshot.exists()) return FirebaseResult.Error(RelevamientoNotFoundException())
            if (snapshot.getString(FIELD_USER_ID) != userId) {
                return FirebaseResult.Error(RelevamientoForbiddenException())
            }
            ref.set(dto.toMap(), SetOptions.merge()).await()
            FirebaseResult.Success(dto.copy(userId = userId, obraId = dto.obraId))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FirebaseResult.Error(e)
        }
    }

    private fun relevamientos(obraId: String) =
        firestore.collection(OBRAS).document(obraId).collection(RELEVAMIENTOS)

    private fun RelevamientoDto.toMap(): Map<String, Any> = mapOf(
        FIELD_USER_ID to userId,
        FIELD_FECHA to Timestamp(Date(fechaMillis)),
        FIELD_NOTA to nota
    )

    private fun DocumentSnapshot.toRelevamientoDto(obraId: String): RelevamientoDto? {
        val ownerId = getString(FIELD_USER_ID) ?: return null
        return RelevamientoDto(
            id = id,
            obraId = obraId,
            userId = ownerId,
            fechaMillis = getTimestamp(FIELD_FECHA)?.toDate()?.time ?: 0L,
            nota = getString(FIELD_NOTA).orEmpty()
        )
    }

    private companion object {
        const val OBRAS = "obras"
        const val RELEVAMIENTOS = "relevamientos"
        const val FIELD_USER_ID = "userId"
        const val FIELD_FECHA = "fecha"
        const val FIELD_NOTA = "nota"
        const val FIELD_ULTIMA_VISITA = "ultimaVisita"
    }
}
