package com.unaj.appmoviles.features.obras.list.components

import com.unaj.appmoviles.obratrack.core.domain.model.Obra
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.EstadoFechaVisita
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.EstadoVisitaConHora
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.estadoFechaVisita
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.estadoVisitaConHora
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.horaVisitaValida
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.toFechaCalendario
import java.time.Clock
import java.time.LocalDate

/** La próxima visita de una obra tal como la muestra su tarjeta. */
sealed interface VisitaDeObra {
    data object SinVisita : VisitaDeObra

    /**
     * [hora]: minutos desde la medianoche, o null si la visita no tiene hora (es opcional).
     * [atrasada]: con hora vence al pasar esa hora; sin hora, al terminar el día (igual que la Agenda).
     */
    data class Programada(val fecha: LocalDate, val hora: Int?, val atrasada: Boolean) : VisitaDeObra
}

fun visitaDeObra(obra: Obra, clock: Clock): VisitaDeObra {
    val proxima = obra.proximaVisita ?: return VisitaDeObra.SinVisita
    val fecha = proxima.toFechaCalendario()
    val hora = horaVisitaValida(obra.horaVisita)
    val atrasada = if (hora != null) {
        estadoVisitaConHora(fecha, hora, clock) == EstadoVisitaConHora.Atrasada
    } else {
        estadoFechaVisita(fecha, LocalDate.now(clock)) is EstadoFechaVisita.Atrasada
    }
    return VisitaDeObra.Programada(fecha, hora, atrasada)
}

/** "12 may 2026 · 10:00", o solo la fecha si no tiene hora. Quien llama pone el formato (idioma y 12/24 h). */
fun VisitaDeObra.Programada.fechaConHora(
    formatearFecha: (LocalDate) -> String,
    formatearHora: (Int) -> String
): String = formatearFecha(fecha) + (hora?.let { " · " + formatearHora(it) } ?: "")
