package com.unaj.appmoviles.obratrack.core.domain.usecase.obras

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** Hora que se asume para armar el instante de una visita que no tiene hora (09:00). */
const val HORA_VISITA_POR_DEFECTO_MINUTOS = 9 * 60

const val MINUTOS_POR_DIA = 24 * 60

/** La hora de una visita son minutos desde la medianoche (0..1439); cualquier otro valor se descarta. */
fun horaVisitaValida(minutos: Int?): Int? = minutos?.takeIf { it in 0 until MINUTOS_POR_DIA }

/** Situación de una visita que tiene hora, respecto del momento actual. */
sealed interface EstadoVisitaConHora {
    /** Ya pasó la hora de la visita. */
    data object Atrasada : EstadoVisitaConHora

    /** Es hoy y todavía no pasó la hora. */
    data object Hoy : EstadoVisitaConHora

    /** [dias]: días de calendario que faltan (>= 1). */
    data class Proxima(val dias: Long) : EstadoVisitaConHora
}

/**
 * Instante de la visita: fecha de calendario + hora en la [zona] del dispositivo.
 * Sin hora ([horaVisitaMinutos] null) se usa [HORA_VISITA_POR_DEFECTO_MINUTOS].
 */
fun instanteVisita(fechaVisita: LocalDate, horaVisitaMinutos: Int?, zona: ZoneId): Instant {
    val minutos = horaVisitaValida(horaVisitaMinutos) ?: HORA_VISITA_POR_DEFECTO_MINUTOS
    return fechaVisita.atTime(LocalTime.of(minutos / 60, minutos % 60)).atZone(zona).toInstant()
}

/**
 * Para visitas CON hora: vence al pasar esa hora (no al terminar el día).
 * Las visitas sin hora se evalúan con [estadoFechaVisita], que vence al terminar el día.
 */
fun estadoVisitaConHora(fechaVisita: LocalDate, horaVisitaMinutos: Int, clock: Clock): EstadoVisitaConHora {
    val instante = instanteVisita(fechaVisita, horaVisitaMinutos, clock.zone)
    if (clock.instant().isAfter(instante)) return EstadoVisitaConHora.Atrasada
    val dias = ChronoUnit.DAYS.between(LocalDate.now(clock), fechaVisita)
    return if (dias <= 0L) EstadoVisitaConHora.Hoy else EstadoVisitaConHora.Proxima(dias)
}
