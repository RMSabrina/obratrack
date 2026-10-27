package com.unaj.appmoviles.obratrack.core.domain.repository

import com.unaj.appmoviles.obratrack.core.domain.error.AppResult

/** Lo que necesita la subida diferida de fotos para completar un relevamiento ya guardado. */
interface FotoRelevamientoRepository {
    suspend fun existe(obraId: String, relevamientoId: String): AppResult<Boolean>

    /** Guarda la URL de la foto. Devuelve false (sin escribir) si el relevamiento ya no existe. */
    suspend fun asignarFotoUrl(obraId: String, relevamientoId: String, url: String): AppResult<Boolean>
}
