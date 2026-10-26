package com.unaj.appmoviles.obratrack.core.domain.usecase.relevamientos

import com.unaj.appmoviles.obratrack.core.domain.model.Relevamiento
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.horaVisitaValida

/** Próxima visita de una obra: fecha de calendario (medianoche UTC) y hora opcional en minutos. */
data class ProximaVisitaObra(val fecha: Long, val hora: Int?)

/**
 * Qué relevamiento define la próxima visita de la obra: el de fecha más reciente entre los que
 * tienen próxima visita (si empatan, el de la próxima visita más lejana). Los que no la tienen
 * no cuentan. Devuelve null si ninguno la tiene.
 */
fun proximaVisitaDeObra(relevamientos: List<Relevamiento>): ProximaVisitaObra? =
    relevamientos
        .filter { it.proximaVisita != null }
        .maxWithOrNull(compareBy<Relevamiento> { it.fecha }.thenBy { it.proximaVisita })
        ?.let { ProximaVisitaObra(fecha = it.proximaVisita!!, hora = horaVisitaValida(it.horaVisita)) }
