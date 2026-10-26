package com.unaj.appmoviles.obratrack.core.domain.usecase

import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.EstadoFechaVisita
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.estadoFechaVisita
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.toFechaCalendario
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.toMillisCalendario
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class EstadoFechaVisitaTest {

    private val buenosAires = ZoneId.of("America/Argentina/Buenos_Aires")
    private val auckland = ZoneId.of("Pacific/Auckland")
    private val diaVisita = LocalDate.of(2026, 10, 26)

    /** Estado de la visita (guardada como fecha de calendario) para un "ahora" local dado. */
    private fun estadoEn(zona: ZoneId, ahoraLocal: LocalDateTime): EstadoFechaVisita {
        val clock = Clock.fixed(ahoraLocal.atZone(zona).toInstant(), zona)
        return estadoFechaVisita(diaVisita.toMillisCalendario().toFechaCalendario(), LocalDate.now(clock))
    }

    @Test
    fun fechaDeCalendario_ida_y_vuelta() {
        assertEquals(diaVisita, diaVisita.toMillisCalendario().toFechaCalendario())
    }

    @Test
    fun diaDeLaVisita_esHoy_a_las0000_y_a_las2359_enAmbasZonas() {
        for (zona in listOf(buenosAires, auckland)) {
            assertEquals(EstadoFechaVisita.Hoy, estadoEn(zona, diaVisita.atTime(LocalTime.MIN)))
            assertEquals(EstadoFechaVisita.Hoy, estadoEn(zona, diaVisita.atTime(23, 59)))
        }
    }

    @Test
    fun diaSiguiente_a_las0000_estaAtrasada_porUnDia() {
        for (zona in listOf(buenosAires, auckland)) {
            assertEquals(
                EstadoFechaVisita.Atrasada(1),
                estadoEn(zona, diaVisita.plusDays(1).atTime(LocalTime.MIN))
            )
        }
    }

    @Test
    fun diaAnterior_a_las2359_faltaUnDia() {
        for (zona in listOf(buenosAires, auckland)) {
            assertEquals(
                EstadoFechaVisita.Proxima(1),
                estadoEn(zona, diaVisita.minusDays(1).atTime(23, 59))
            )
        }
    }

    @Test
    fun conVariosDiasDeDiferencia_cuentaLosDias() {
        assertEquals(EstadoFechaVisita.Atrasada(3), estadoFechaVisita(diaVisita.minusDays(3), diaVisita))
        assertEquals(EstadoFechaVisita.Proxima(10), estadoFechaVisita(diaVisita.plusDays(10), diaVisita))
    }
}
