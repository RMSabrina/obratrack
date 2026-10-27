package com.unaj.appmoviles.obratrack.core.firebase

import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Las escrituras de Firestore se aplican primero en el caché local y el Task solo termina cuando
 * el servidor responde: sin red nunca termina. Por eso no se esperan, y un rechazo posterior del
 * servidor (por ejemplo, reglas) llega acá para que la app lo muestre al usuario.
 */
@Singleton
class FallosEscritura @Inject constructor() {
    private val _fallos = MutableSharedFlow<Exception>(
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val fallos: Flow<Exception> = _fallos.asSharedFlow()

    fun reportar(e: Exception) {
        _fallos.tryEmit(e)
    }
}

/** Lanza la escritura sin esperar al servidor; si este la rechaza, se informa en [fallos]. */
internal fun Task<*>.sinEsperar(fallos: FallosEscritura) {
    // Ejecutor directo: el listener solo emite en un flow, no hace falta el hilo principal.
    addOnFailureListener({ it.run() }) { e -> fallos.reportar(e) }
}

/**
 * Lee un documento solo para verificar existencia y dueño. Primero el caché (sin red ni demora);
 * si no está, la fuente por defecto. Devuelve null si no hay red y el documento nunca se cacheó:
 * en ese caso no se puede verificar y las reglas del servidor deciden al sincronizar.
 */
internal suspend fun DocumentReference.getParaVerificar(): DocumentSnapshot? {
    try {
        val enCache = get(Source.CACHE).await()
        if (enCache.exists()) return enCache
    } catch (e: FirebaseFirestoreException) {
        // No está en el caché: se sigue con la fuente por defecto.
    }
    return try {
        get().await()
    } catch (e: FirebaseFirestoreException) {
        if (e.code == FirebaseFirestoreException.Code.UNAVAILABLE) null else throw e
    }
}
