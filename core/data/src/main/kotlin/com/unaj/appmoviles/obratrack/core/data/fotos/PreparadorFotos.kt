package com.unaj.appmoviles.obratrack.core.data.fotos

import com.unaj.appmoviles.obratrack.core.data.repository.FotoPreparada
import com.unaj.appmoviles.obratrack.core.data.repository.prepararFoto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** Achica la foto antes de guardarla en la cola; null si no se puede dejar por debajo del tope. */
interface PreparadorFotos {
    suspend fun preparar(bytes: ByteArray): ByteArray?
}

class PreparadorFotosAndroid @Inject constructor() : PreparadorFotos {
    // Achicar es pesado: va en IO y no en el hilo principal.
    override suspend fun preparar(bytes: ByteArray): ByteArray? = withContext(Dispatchers.IO) {
        when (val preparada = prepararFoto(bytes)) {
            is FotoPreparada.Lista -> preparada.bytes
            FotoPreparada.DemasiadoGrande -> null
        }
    }
}
