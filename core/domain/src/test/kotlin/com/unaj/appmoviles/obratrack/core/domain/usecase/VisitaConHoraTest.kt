package com.unaj.appmoviles.obratrack.core.domain.usecase

import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.EstadoVisitaConHora
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.HORA_VISITA_POR_DEFECTO_MINUTOS
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.estadoVisitaConHora
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.horaVisitaValida
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.instanteVisita
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class VisitaConHoraTest {

    private val buenosAires = ZoneId.of("America/Argentina/Buenos_Aires")
    private val auckland = ZoneId.of("Pacific/Auckland")
    private val zonas = listOf(buenosAires, auckland)
    private val diaVisita = LocalDate.of(2026, 10, 26)
    private val tresDeLaTarde = 15 * 60

    private fun estadoEn(zona: ZoneId, ahoraLocal: LocalDateTime, hora: Int = tresDeLaTarde): EstadoVisitaConHora {
        val clock = Clock.fixed(ahoraLocal.atZone(zona).toInstant(), zona)
        return estadoVisitaConHora(diaVisita, hora, clock)
    }

    @Test
    fun horaVisitaValida_aceptaSoloMinutosDelDia() {
        assertNull(horaVisitaValida(null))
        assertNull(horaVisitaValida(-1))
        assertNull(horaVisitaValida(1440))
        assertEquals(0, horaVisitaValida(0))
        assertEquals(1439, horaVisitaValida(1439))
    }

    @Test
    fun instanteVisita_sinHora_usaLas0900LocalesDeCadaZona() {
        assertEquals(
            Instant.parse("2026-10-26T12:00:00Z"),
            instanteVisita(diaVisita, null, buenosAires)
        )
        assertEquals(
            Instant.parse("2026-10-25T20:00:00Z"),
            instanteVisita(diaVisita, null, auckland)
        )
        assertEquals(9 * 60, HORA_VISITA_POR_DEFECTO_MINUTOS)
    }

    @Test
    fun instanteVisita_conHora_usaLaZonaLocal() {
        val hora = 15 * 60 + 30
        assertEquals(
            Instant.parse("2026-10-26T18:30:00Z"),
            instanteVisita(diaVisita, hora, buenosAires)
        )
        assertEquals(
            Instant.parse("2026-10-26T02:30:00Z"),
            instanteVisita(diaVisita, hora, auckland)
        )
    }

    @Test
    fun instanteVisita_horaInvalida_cuentaComoSinHora() {
        assertEquals(
            instanteVisita(diaVisita, null, buenosAires),
            instanteVisita(diaVisita, 5000, buenosAires)
        )
    }

    @Test
    fun antesDeLaHora_esHoy_yEnLaHoraExacta_todaviaEsHoy() {
        for (zona in zonas) {
            assertEquals(EstadoVisitaConHora.Hoy, estadoEn(zona, diaVisita.atTime(0, 0)))
            assertEquals(EstadoVisitaConHora.Hoy, estadoEn(zona, diaVisita.atTime(14, 59)))
            assertEquals(EstadoVisitaConHora.Hoy, estadoEn(zona, diaVisita.atTime(15, 0)))
        }
    }

    @Test
    fun alPasarLaHora_estaAtrasada_aunqueSigaSiendoElMismoDia() {
        for (zona in zonas) {
            assertEquals(EstadoVisitaConHora.Atrasada, estadoEn(zona, diaVisita.atTime(15, 1)))
            assertEquals(EstadoVisitaConHora.Atrasada, estadoEn(zona, diaVisita.atTime(23, 59)))
        }
    }

    @Test
    fun cruceDeDia_visitaAlFinalDelDia_vencePasadaLaMedianoche() {
        val ultimoMinuto = 23 * 60 + 59
        for (zona in zonas) {
            assertEquals(EstadoVisitaConHora.Hoy, estadoEn(zona, diaVisita.atTime(23, 59), ultimoMinuto))
            assertEquals(
                EstadoVisitaConHora.Atrasada,
                estadoEn(zona, diaVisita.plusDays(1).atTime(0, 0), ultimoMinuto)
            )
        }
    }

    @Test
    fun diasAntes_faltanDiasDeCalendario() {
        for (zona in zonas) {
            assertEquals(EstadoVisitaConHora.Proxima(1), estadoEn(zona, diaVisita.minusDays(1).atTime(23, 59)))
            assertEquals(EstadoVisitaConHora.Proxima(3), estadoEn(zona, diaVisita.minusDays(3).atTime(8, 0)))
        }
    }
}
