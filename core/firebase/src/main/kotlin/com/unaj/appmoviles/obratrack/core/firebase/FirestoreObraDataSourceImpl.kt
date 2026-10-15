package com.unaj.appmoviles.obratrack.core.firebase

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date
import kotlin.coroutines.cancellation.CancellationException

class FirestoreObraDataSourceImpl(
    private val firestore: FirebaseFirestore
) : FirestoreObraDataSource {

    override fun observe(userId: String): Flow<List<ObraDto>> = callbackFlow {
        val registration = firestore.collection(COLLECTION)
            .whereEqualTo(FIELD_USER_ID, userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val obras = snapshot?.documents?.mapNotNull { it.toObraDto() }.orEmpty()
                trySend(obras)
            }
        awaitClose { registration.remove() }
    }

    override fun observeOne(userId: String, obraId: String): Flow<ObraDto?> = callbackFlow {
        val registration = firestore.collection(COLLECTION).document(obraId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val obra = snapshot?.toObraDto()
                trySend(if (obra?.userId == userId) obra else null)
            }
        awaitClose { registration.remove() }
    }

    override fun observePublicas(): Flow<List<ObraDto>> = callbackFlow {
        val registration = firestore.collection(COLLECTION)
            .whereEqualTo(FIELD_PUBLICA, true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val obras = snapshot?.documents?.mapNotNull { it.toObraDto() }.orEmpty()
                trySend(obras)
            }
        awaitClose { registration.remove() }
    }

    override fun observePublica(obraId: String): Flow<ObraDto?> = callbackFlow {
        val registration = firestore.collection(COLLECTION).document(obraId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val obra = snapshot?.toObraDto()
                trySend(if (obra?.publica == true) obra else null)
            }
        awaitClose { registration.remove() }
    }

    override suspend fun create(dto: ObraDto): FirebaseResult<ObraDto> {
        return try {
            val ref = firestore.collection(COLLECTION).document()
            val stored = dto.copy(id = ref.id)
            ref.set(stored.toCreateMap()).await()
            FirebaseResult.Success(stored)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FirebaseResult.Error(e)
        }
    }

    override suspend fun update(userId: String, obraId: String, dto: ObraDto): FirebaseResult<ObraDto> {
        return try {
            val ref = firestore.collection(COLLECTION).document(obraId)
            val snapshot = ref.get().await()
            if (!snapshot.exists()) return FirebaseResult.Error(ObraNotFoundException())
            if (snapshot.getString(FIELD_USER_ID) != userId) {
                return FirebaseResult.Error(ObraForbiddenException())
            }
            ref.update(dto.toUpdateMap()).await()
            FirebaseResult.Success(
                dto.copy(
                    id = obraId,
                    userId = userId,
                    fechaInicioMillis = snapshot.getTimestamp(FIELD_FECHA_INICIO)?.toDate()?.time
                        ?: dto.fechaInicioMillis,
                    ultimaVisitaMillis = snapshot.getTimestamp(FIELD_ULTIMA_VISITA)?.toDate()?.time,
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FirebaseResult.Error(e)
        }
    }

    private fun ObraDto.toCreateMap(): Map<String, Any> = mapOf(
        FIELD_USER_ID to userId,
        FIELD_NOMBRE to nombre,
        FIELD_CLIENTE to cliente,
        FIELD_PUBLICA to publica,
        FIELD_DIRECCION to direccion,
        FIELD_FECHA_INICIO to Timestamp(Date(fechaInicioMillis)),
        FIELD_ESTADO to estado,
    )

    private fun ObraDto.toUpdateMap(): Map<String, Any> = mapOf(
        FIELD_NOMBRE to nombre,
        FIELD_CLIENTE to cliente,
        FIELD_PUBLICA to publica,
        FIELD_DIRECCION to direccion,
        FIELD_ESTADO to estado
    )

    private fun DocumentSnapshot.toObraDto(): ObraDto? {
        val ownerId = getString(FIELD_USER_ID) ?: return null
        return ObraDto(
            id = id,
            userId = ownerId,
            nombre = getString(FIELD_NOMBRE).orEmpty(),
            cliente = getString(FIELD_CLIENTE).orEmpty(),
            publica = getBoolean(FIELD_PUBLICA) ?: false,
            direccion = getString(FIELD_DIRECCION).orEmpty(),
            fechaInicioMillis = getTimestamp(FIELD_FECHA_INICIO)?.toDate()?.time ?: 0L,
            estado = getString(FIELD_ESTADO) ?: ESTADO_ACTIVA,
            ultimaVisitaMillis = getTimestamp(FIELD_ULTIMA_VISITA)?.toDate()?.time,
        )
    }


    private companion object {
        const val COLLECTION = "obras"
        const val FIELD_USER_ID = "userId"
        const val FIELD_NOMBRE = "nombre"
        const val FIELD_CLIENTE = "cliente"
        const val FIELD_PUBLICA = "publica"
        const val FIELD_DIRECCION = "direccion"
        const val FIELD_FECHA_INICIO = "fechaInicio"
        const val FIELD_ESTADO = "estado"
        const val FIELD_ULTIMA_VISITA = "ultimaVisita"
        const val ESTADO_ACTIVA = "activa"
    }
}
