package com.unaj.appmoviles.obratrack.core.domain.usecase.obras

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

/** Situación de una visita respecto de "hoy". Se compara siempre por día de calendario. */
sealed interface EstadoFechaVisita {
    /** [dias]: días de atraso (>= 1). */
    data class Atrasada(val dias: Long) : EstadoFechaVisita

    data object Hoy : EstadoFechaVisita

    /** [dias]: días que faltan (>= 1). */
    data class Proxima(val dias: Long) : EstadoFechaVisita
}

fun estadoFechaVisita(fechaVisita: LocalDate, hoy: LocalDate): EstadoFechaVisita {
    val dias = ChronoUnit.DAYS.between(hoy, fechaVisita)
    return when {
        dias < 0 -> EstadoFechaVisita.Atrasada(-dias)
        dias == 0L -> EstadoFechaVisita.Hoy
        else -> EstadoFechaVisita.Proxima(dias)
    }
}

/**
 * Las fechas de calendario (proximaVisita, fecha de relevamiento, fechaInicio) se guardan como la
 * medianoche UTC del día elegido: se leen en UTC. "Hoy" sí sale de un Clock en la zona local.
 */
fun Long.toFechaCalendario(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

/** Inversa de [toFechaCalendario]. */
fun LocalDate.toMillisCalendario(): Long =
    atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
