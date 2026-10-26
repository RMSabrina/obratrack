package com.unaj.appmoviles.obratrack.core.domain.usecase.obras

import com.unaj.appmoviles.obratrack.core.domain.model.AnticipacionAviso
import java.time.Duration
import java.time.Instant

/** Qué hacer con el aviso de una visita. */
sealed interface PlanAviso {
    /** Avisar en [instante], que está en el futuro. */
    data class Programar(val instante: Instant) : PlanAviso

    /** El momento del aviso ya pasó pero la visita no: avisar enseguida. */
    data object AvisarYa : PlanAviso

    /** La visita ya pasó: no se avisa (queda como atrasada). */
    data object Ninguno : PlanAviso
}

/** Instante en que corresponde avisar: el de la visita menos la anticipación. */
fun instanteAviso(instanteVisita: Instant, anticipacion: AnticipacionAviso): Instant =
    instanteVisita.minus(Duration.ofMinutes(anticipacion.minutos))

/** Nunca programa en el pasado: si el aviso ya venció y la visita no, devuelve [PlanAviso.AvisarYa]. */
fun planificarAviso(ahora: Instant, instanteVisita: Instant, anticipacion: AnticipacionAviso): PlanAviso {
    if (!instanteVisita.isAfter(ahora)) return PlanAviso.Ninguno
    val aviso = instanteAviso(instanteVisita, anticipacion)
    return if (aviso.isAfter(ahora)) PlanAviso.Programar(aviso) else PlanAviso.AvisarYa
}
