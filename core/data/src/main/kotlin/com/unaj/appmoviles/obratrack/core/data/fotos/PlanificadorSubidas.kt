package com.unaj.appmoviles.obratrack.core.data.fotos

import kotlinx.coroutines.flow.Flow

data class SubidaProgramada(val obraId: String, val relevamientoId: String)

/**
 * Agenda la subida de una foto guardada; la implementación real usa WorkManager (vive en :app,
 * que es donde está esa dependencia). Una subida por relevamiento: programar de nuevo la reemplaza.
 */
interface PlanificadorSubidas {
    /** Subidas que todavía no terminaron (en espera de red, reintentando o subiendo). */
    val programadas: Flow<Set<SubidaProgramada>>

    fun programar(obraId: String, relevamientoId: String)
    fun cancelar(relevamientoId: String)
    fun cancelarDeObra(obraId: String)
}
