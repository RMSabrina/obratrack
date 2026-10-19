package com.unaj.appmoviles.obratrack.core.firebase

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date
import kotlin.coroutines.cancellation.CancellationException
import javax.inject.Inject

class FirestoreRelevamientoDataSourceImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : FirestoreRelevamientoDataSource {

    override fun observeAll(userId: String): Flow<List<RelevamientoDto>> = callbackFlow {
        val registration = firestore.collectionGroup(RELEVAMIENTOS)
            .whereEqualTo(FIELD_USER_ID, userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirestoreDS", "Error in observeAll", error)
                    close(error)
                    return@addSnapshotListener
                }
                Log.d("FirestoreDS", "snapshot has ${snapshot?.documents?.size} docs")
                val items = snapshot?.documents?.mapNotNull { it.toRelevamientoDtoFromGroup() }.orEmpty()
                Log.d("FirestoreDS", "mapped ${items.size} items")
                trySend(items)
            }
        awaitClose { registration.remove() }
    }

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

    override fun observeByObra(obraId: String): Flow<List<RelevamientoDto>> = callbackFlow {
        val registration = relevamientos(obraId)
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

    override suspend fun create(dto: RelevamientoDto, photo: ByteArray?): FirebaseResult<RelevamientoDto> {
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

    override suspend fun update(
        userId: String,
        dto: RelevamientoDto,
        photo: ByteArray?
    ): FirebaseResult<RelevamientoDto> {
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
            val stored = dto.copy(userId = userId)
            ref.set(stored.toMap(), SetOptions.merge()).await()
            FirebaseResult.Success(stored)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FirebaseResult.Error(e)
        }
    }

    private fun relevamientos(obraId: String) =
        firestore.collection(OBRAS).document(obraId).collection(RELEVAMIENTOS)

    private fun RelevamientoDto.toMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            FIELD_USER_ID to userId,
            FIELD_FECHA to Timestamp(Date(fechaMillis)),
            FIELD_NOTA to nota
        )
        fotoUrl?.let { map[FIELD_FOTO_URL] = it }
        if (latitud != null && longitud != null) {
            map[FIELD_UBICACION] = GeoPoint(latitud, longitud)
        }
        return map
    }

    private fun DocumentSnapshot.toRelevamientoDto(obraId: String): RelevamientoDto? {
        val ownerId = getString(FIELD_USER_ID) ?: return null
        val geo = getGeoPoint(FIELD_UBICACION)
        return RelevamientoDto(
            id = id,
            obraId = obraId,
            userId = ownerId,
            fechaMillis = getTimestamp(FIELD_FECHA)?.toDate()?.time ?: 0L,
            nota = getString(FIELD_NOTA).orEmpty(),
            fotoUrl = getString(FIELD_FOTO_URL),
            latitud = geo?.latitude,
            longitud = geo?.longitude
        )
    }

    private fun DocumentSnapshot.toRelevamientoDtoFromGroup(): RelevamientoDto? {
        val ownerId = getString(FIELD_USER_ID)
        if (ownerId == null) {
            Log.e("FirestoreDS", "ownerId is null for doc ${id}")
            return null
        }
        val geo = getGeoPoint(FIELD_UBICACION)
        val obraId = reference.parent.parent?.id
        if (obraId == null) {
            Log.e("FirestoreDS", "obraId is null for doc ${id}")
            return null
        }
        return RelevamientoDto(
            id = id,
            obraId = obraId,
            userId = ownerId,
            fechaMillis = getTimestamp(FIELD_FECHA)?.toDate()?.time ?: 0L,
            nota = getString(FIELD_NOTA).orEmpty(),
            fotoUrl = getString(FIELD_FOTO_URL),
            latitud = geo?.latitude,
            longitud = geo?.longitude
        )
    }

    private companion object {
        const val OBRAS = "obras"
        const val RELEVAMIENTOS = "relevamientos"
        const val FIELD_USER_ID = "userId"
        const val FIELD_FECHA = "fecha"
        const val FIELD_NOTA = "nota"
        const val FIELD_FOTO_URL = "fotoUrl"
        const val FIELD_UBICACION = "ubicacion"
        const val FIELD_ULTIMA_VISITA = "ultimaVisita"
    }
}
