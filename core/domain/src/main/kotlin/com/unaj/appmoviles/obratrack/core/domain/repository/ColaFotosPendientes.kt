package com.unaj.appmoviles.obratrack.core.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Fotos de relevamientos guardadas en el teléfono que todavía no se subieron a Cloudinary. Se suben
 * solas cuando hay red y, al terminar, se asignan al relevamiento.
 */
interface ColaFotosPendientes {
    /** Id del relevamiento → ruta del archivo local con su foto en espera. */
    val pendientes: Flow<Map<String, String>>

    /** Guarda la foto y programa su subida; reemplaza la que ya estuviera en espera para ese relevamiento. */
    suspend fun encolar(obraId: String, relevamientoId: String, bytes: ByteArray)

    /** Descarta la foto en espera de un relevamiento (se borró o se le quitó la foto). */
    suspend fun cancelar(obraId: String, relevamientoId: String)

    /** Descarta todas las fotos en espera de una obra (se borró la obra). */
    suspend fun cancelarDeObra(obraId: String)
}
