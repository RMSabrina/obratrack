package com.unaj.appmoviles.obratrack.core.firebase

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

/**
 * Ids de documentos con escrituras locales sin confirmar. Escucha con [MetadataChanges.INCLUDE]
 * (para enterarse de la confirmación del servidor) en listeners aparte de los de datos, así los
 * listados no reciben emisiones extra; y solo emite cuando el conjunto cambia.
 */
class FirestorePendientesDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    fun obras(userId: String): Flow<Set<String>> =
        pendientes(firestore.collection(OBRAS).whereEqualTo(FIELD_USER_ID, userId))

    fun relevamientos(obraId: String): Flow<Set<String>> =
        pendientes(firestore.collection(OBRAS).document(obraId).collection(RELEVAMIENTOS))

    fun tareas(obraId: String): Flow<Set<String>> =
        pendientes(firestore.collection(OBRAS).document(obraId).collection(TAREAS))

    private fun pendientes(query: Query): Flow<Set<String>> = callbackFlow {
        val registration = query.addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val ids = snapshot?.documents
                ?.filter { it.metadata.hasPendingWrites() }
                ?.map { it.id }
                ?.toSet()
                .orEmpty()
            trySend(ids)
        }
        awaitClose { registration.remove() }
    }.distinctUntilChanged()

    private companion object {
        const val OBRAS = "obras"
        const val RELEVAMIENTOS = "relevamientos"
        const val TAREAS = "tareas"
        const val FIELD_USER_ID = "userId"
    }
}
