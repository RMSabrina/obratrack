package com.unaj.appmoviles.obratrack.core.firebase

import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/** Completa un relevamiento ya guardado con la URL de su foto, una vez subida (cola de fotos). */
class FirestoreFotoDataSource @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val fallos: FallosEscritura
) {
    /** Sin red y sin el documento en el caché no se puede saber: se asume que existe. */
    suspend fun existe(obraId: String, relevamientoId: String): FirebaseResult<Boolean> = try {
        FirebaseResult.Success(referencia(obraId, relevamientoId).getParaVerificar()?.exists() ?: true)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        FirebaseResult.Error(e)
    }

    suspend fun asignarFotoUrl(obraId: String, relevamientoId: String, url: String): FirebaseResult<Boolean> = try {
        val ref = referencia(obraId, relevamientoId)
        if (ref.getParaVerificar()?.exists() == false) {
            FirebaseResult.Success(false)
        } else {
            ref.update(FIELD_FOTO_URL, url).sinEsperar(fallos)
            FirebaseResult.Success(true)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        FirebaseResult.Error(e)
    }

    private fun referencia(obraId: String, relevamientoId: String) =
        firestore.collection(OBRAS).document(obraId).collection(RELEVAMIENTOS).document(relevamientoId)

    private companion object {
        const val OBRAS = "obras"
        const val RELEVAMIENTOS = "relevamientos"
        const val FIELD_FOTO_URL = "fotoUrl"
    }
}
