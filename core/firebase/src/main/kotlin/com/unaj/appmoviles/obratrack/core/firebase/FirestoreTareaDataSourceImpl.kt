package com.unaj.appmoviles.obratrack.core.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.cancellation.CancellationException
import javax.inject.Inject

class FirestoreTareaDataSourceImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : FirestoreTareaDataSource {

    override fun observeByObra(obraId: String): Flow<List<TareaDto>> = callbackFlow {
        val registration = tareas(obraId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val items = snapshot?.documents?.mapNotNull { it.toTareaDto(obraId) }.orEmpty()
                trySend(items)
            }
        awaitClose { registration.remove() }
    }

    override fun observeOne(obraId: String, tareaId: String): Flow<TareaDto?> = callbackFlow {
        val registration = tareas(obraId).document(tareaId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.takeIf { it.exists() }?.toTareaDto(obraId))
            }
        awaitClose { registration.remove() }
    }

    override suspend fun create(dto: TareaDto): FirebaseResult<TareaDto> {
        return try {
            val obra = firestore.collection(OBRAS).document(dto.obraId).get().await()
            if (!obra.exists()) return FirebaseResult.Error(ObraNotFoundException())
            if (obra.getString(FIELD_USER_ID) != dto.userId) {
                return FirebaseResult.Error(ObraForbiddenException())
            }

            val ref = tareas(dto.obraId).document()
            val stored = dto.copy(id = ref.id)
            ref.set(stored.toMap()).await()
            FirebaseResult.Success(stored)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FirebaseResult.Error(e)
        }
    }

    override suspend fun update(userId: String, dto: TareaDto): FirebaseResult<TareaDto> {
        return try {
            val obra = firestore.collection(OBRAS).document(dto.obraId).get().await()
            if (!obra.exists()) return FirebaseResult.Error(ObraNotFoundException())
            if (obra.getString(FIELD_USER_ID) != userId) {
                return FirebaseResult.Error(ObraForbiddenException())
            }

            val ref = tareas(dto.obraId).document(dto.id)
            val snapshot = ref.get().await()
            if (!snapshot.exists()) return FirebaseResult.Error(TareaNotFoundException())
            if (snapshot.getString(FIELD_USER_ID) != userId) {
                return FirebaseResult.Error(TareaForbiddenException())
            }
            val stored = dto.copy(userId = userId)
            // toMap() escribe las fechas null explícitamente para que merge las borre.
            ref.set(stored.toMap(), SetOptions.merge()).await()
            FirebaseResult.Success(stored)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FirebaseResult.Error(e)
        }
    }

    override suspend fun delete(
        obraId: String,
        tareaId: String,
        eliminarRelevamientos: Boolean
    ): FirebaseResult<Unit> {
        return try {
            if (auth.uid == null) return FirebaseResult.Error(TareaForbiddenException())
            // Del servidor y no del caché: sin conexión falla en vez de dejar vínculos sin ver.
            val vinculados = firestore.collection(OBRAS).document(obraId).collection(RELEVAMIENTOS)
                .whereEqualTo(RELEVAMIENTO_FIELD_TAREA_ID, tareaId)
                .get(Source.SERVER).await()
                .documents
            // Un batch admite hasta 500 escrituras: los vinculados + la tarea + la obra (su última
            // visita, si se borran relevamientos). Partirlo dejaría de ser atómico, así que se rechaza
            // (no se espera llegar a este número).
            val borraRelevamientos = eliminarRelevamientos && vinculados.isNotEmpty()
            if (vinculados.size + (if (borraRelevamientos) 2 else 1) > BATCH_SIZE) {
                return FirebaseResult.Error(DemasiadosVinculadosException())
            }
            // Al borrar relevamientos, la última visita de la obra pasa a la del más reciente que queda.
            // Se lee del servidor, igual que los vinculados: acá no hay caché incompleto que valga.
            val ultimaVisita = if (borraRelevamientos) {
                val todos = fechasPorId(
                    firestore.collection(OBRAS).document(obraId).collection(RELEVAMIENTOS)
                        .get(Source.SERVER).await().documents
                )
                ultimaVisitaTras(todos, quitar = vinculados.map { it.id }.toSet())
            } else {
                null
            }
            firestore.batch().apply {
                vinculados.forEach { doc ->
                    if (eliminarRelevamientos) {
                        delete(doc.reference)
                    } else {
                        update(doc.reference, RELEVAMIENTO_FIELD_TAREA_ID, FieldValue.delete())
                    }
                }
                delete(tareas(obraId).document(tareaId))
                if (borraRelevamientos) {
                    update(
                        firestore.collection(OBRAS).document(obraId),
                        OBRA_FIELD_ULTIMA_VISITA,
                        valorUltimaVisita(ultimaVisita)
                    )
                }
            }.commit().await()
            FirebaseResult.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FirebaseResult.Error(e)
        }
    }

    override suspend fun deleteAllByObra(obraId: String): FirebaseResult<Unit> {
        return try {
            if (auth.uid == null) return FirebaseResult.Error(TareaForbiddenException())
            val docs = tareas(obraId).get(Source.SERVER).await().documents
            docs.chunked(BATCH_SIZE).forEach { chunk ->
                val batch = firestore.batch()
                chunk.forEach { doc -> batch.delete(doc.reference) }
                batch.commit().await()
            }
            FirebaseResult.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FirebaseResult.Error(e)
        }
    }

    private fun tareas(obraId: String) =
        firestore.collection(OBRAS).document(obraId).collection(TAREAS)

    private fun TareaDto.toMap(): Map<String, Any?> = mapOf(
        FIELD_USER_ID to userId,
        FIELD_TITULO to titulo,
        FIELD_DESCRIPCION to descripcion,
        FIELD_CATEGORIA to categoria,
        FIELD_ESTADO to estado,
        FIELD_FECHA_INICIO to fechaInicio,
        FIELD_FECHA_FIN to fechaFin
    )

    private fun DocumentSnapshot.toTareaDto(obraId: String): TareaDto? {
        val ownerId = getString(FIELD_USER_ID) ?: return null
        return TareaDto(
            id = id,
            obraId = obraId,
            userId = ownerId,
            titulo = getString(FIELD_TITULO).orEmpty(),
            descripcion = getString(FIELD_DESCRIPCION).orEmpty(),
            categoria = getString(FIELD_CATEGORIA).orEmpty(),
            estado = getString(FIELD_ESTADO).orEmpty(),
            fechaInicio = getString(FIELD_FECHA_INICIO),
            fechaFin = getString(FIELD_FECHA_FIN)
        )
    }

    private companion object {
        const val BATCH_SIZE = 500
        const val OBRAS = "obras"
        const val TAREAS = "tareas"
        const val RELEVAMIENTOS = "relevamientos"
        const val FIELD_USER_ID = "userId"
        const val FIELD_TITULO = "titulo"
        const val FIELD_DESCRIPCION = "descripcion"
        const val FIELD_CATEGORIA = "categoria"
        const val FIELD_ESTADO = "estado"
        const val FIELD_FECHA_INICIO = "fechaInicio"
        const val FIELD_FECHA_FIN = "fechaFin"
    }
}
