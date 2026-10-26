package com.unaj.appmoviles.obratrack.core.firebase

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.Source
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date
import kotlin.coroutines.cancellation.CancellationException
import javax.inject.Inject

class FirestoreObraDataSourceImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
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
            // Fecha y hora van juntas: sin fecha se borran ambas, sin hora se borra la hora (nunca un null explícito).
            val campos = dto.toUpdateMap().toMutableMap<String, Any>()
            if (dto.proximaVisitaMillis == null) campos[FIELD_PROXIMA_VISITA] = FieldValue.delete()
            if (dto.proximaVisitaMillis == null || dto.horaVisita == null) campos[FIELD_HORA_VISITA] = FieldValue.delete()
            ref.update(campos).await()
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

    private fun ObraDto.toCreateMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            FIELD_USER_ID to userId,
            FIELD_NOMBRE to nombre,
            FIELD_CLIENTE to cliente,
            FIELD_PUBLICA to publica,
            FIELD_DIRECCION to direccion,
            FIELD_FECHA_INICIO to Timestamp(Date(fechaInicioMillis)),
            FIELD_ESTADO to estado,
        )
        if (latitud != null && longitud != null) {
            map[FIELD_UBICACION] = GeoPoint(latitud, longitud)
        }
        if (proximaVisitaMillis != null) {
            map[FIELD_PROXIMA_VISITA] = Timestamp(Date(proximaVisitaMillis))
            if (horaVisita != null) map[FIELD_HORA_VISITA] = horaVisita
        }
        return map
    }

    private fun ObraDto.toUpdateMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            FIELD_NOMBRE to nombre,
            FIELD_CLIENTE to cliente,
            FIELD_PUBLICA to publica,
            FIELD_DIRECCION to direccion,
            FIELD_ESTADO to estado
        )
        if (latitud != null && longitud != null) {
            map[FIELD_UBICACION] = GeoPoint(latitud, longitud)
        }
        if (proximaVisitaMillis != null) {
            map[FIELD_PROXIMA_VISITA] = Timestamp(Date(proximaVisitaMillis))
            if (horaVisita != null) map[FIELD_HORA_VISITA] = horaVisita
        }
        return map
    }

    private fun DocumentSnapshot.toObraDto(): ObraDto? {
        val ownerId = getString(FIELD_USER_ID) ?: return null
        val geo = getGeoPoint(FIELD_UBICACION)
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
            latitud = geo?.latitude,
            longitud = geo?.longitude,
            proximaVisitaMillis = getTimestamp(FIELD_PROXIMA_VISITA)?.toDate()?.time,
            horaVisita = getLong(FIELD_HORA_VISITA)?.toInt()
        )
    }

    override suspend fun existsAndOwnedByCurrentUser(obraId: String): FirebaseResult<Boolean> {
        return try {
            val uid = auth.uid ?: return FirebaseResult.Error(ObraForbiddenException())
            val querySnapshot = firestore.collection(COLLECTION)
                .whereEqualTo(FieldPath.documentId(), obraId)
                .whereEqualTo(FIELD_USER_ID, uid)
                .get(Source.SERVER)
                .await()
            FirebaseResult.Success(!querySnapshot.isEmpty)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FirebaseResult.Error(e)
        }
    }

    override suspend fun delete(obraId: String): FirebaseResult<Unit> {
        return try {
            if (auth.uid == null) return FirebaseResult.Error(ObraForbiddenException())
            firestore.collection(COLLECTION).document(obraId).delete().await()
            FirebaseResult.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FirebaseResult.Error(e)
        }
    }

    override suspend fun deleteAllSuscriptoresByObra(obraId: String): FirebaseResult<Unit> {
        return try {
            if (auth.uid == null) return FirebaseResult.Error(ObraForbiddenException())
            val snapshot = firestore.collection(COLLECTION)
                .document(obraId)
                .collection(COLLECTION_SUSCRIPTORES)
                .get(Source.SERVER)
                .await()
            val docs = snapshot.documents
            if (docs.isNotEmpty()) {
                docs.chunked(BATCH_SIZE).forEach { chunk ->
                    val batch = firestore.batch()
                    chunk.forEach { doc -> batch.delete(doc.reference) }
                    batch.commit().await()
                }
            }
            FirebaseResult.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FirebaseResult.Error(e)
        }
    }

    private companion object {
        const val BATCH_SIZE = 500
        const val COLLECTION = "obras"
        const val COLLECTION_SUSCRIPTORES = "suscriptores"
        const val FIELD_USER_ID = "userId"
        const val FIELD_NOMBRE = "nombre"
        const val FIELD_CLIENTE = "cliente"
        const val FIELD_PUBLICA = "publica"
        const val FIELD_DIRECCION = "direccion"
        const val FIELD_FECHA_INICIO = "fechaInicio"
        const val FIELD_ESTADO = "estado"
        const val FIELD_ULTIMA_VISITA = "ultimaVisita"
        const val FIELD_UBICACION = "ubicacion"
        const val FIELD_PROXIMA_VISITA = "proximaVisita"
        const val FIELD_HORA_VISITA = "horaVisita"
        const val ESTADO_ACTIVA = "activa"
    }
}
