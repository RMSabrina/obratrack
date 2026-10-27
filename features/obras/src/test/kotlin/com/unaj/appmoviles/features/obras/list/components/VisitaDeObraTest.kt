package com.unaj.appmoviles.features.obras.list.components

import com.unaj.appmoviles.obratrack.core.domain.model.EstadoObra
import com.unaj.appmoviles.obratrack.core.domain.model.Obra
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.toMillisCalendario
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class VisitaDeObraTest {

    private val zona = ZoneId.of("America/Argentina/Buenos_Aires")
    private val hoy = LocalDate.of(2026, 10, 22)

    /** Reloj fijo en [hoy] a la [hora] (minutos desde la medianoche), en la zona local. */
    private fun relojA(hora: Int): Clock =
        Clock.fixed(hoy.atStartOfDay(zona).plusMinutes(hora.toLong()).toInstant(), zona)

    private fun obra(fecha: LocalDate?, hora: Int? = null) = Obra(
        id = "o1",
        userId = "u1",
        nombre = "Obra",
        cliente = "Cliente",
        direccion = "Calle 1",
        publica = false,
        estado = EstadoObra.ACTIVA,
        fechaInicio = 0L,
        ultimaVisita = null,
        proximaVisita = fecha?.toMillisCalendario(),
        horaVisita = hora
    )

    private val formatearFecha: (LocalDate) -> String = { it.toString() }
    private val formatearHora: (Int) -> String = { "%02d:%02d".format(it / 60, it % 60) }

    @Test
    fun sinVisita() {
        assertEquals(VisitaDeObra.SinVisita, visitaDeObra(obra(fecha = null), relojA(10 * 60)))
    }

    @Test
    fun sinVisita_ignoraUnaHoraSuelta() {
        assertEquals(VisitaDeObra.SinVisita, visitaDeObra(obra(fecha = null, hora = 600), relojA(10 * 60)))
    }

    @Test
    fun fechaSinHora_futura_noEstaAtrasada_yElTextoEsSoloLaFecha() {
        val visita = visitaDeObra(obra(hoy.plusDays(3)), relojA(10 * 60)) as VisitaDeObra.Programada

        assertEquals(null, visita.hora)
        assertEquals(false, visita.atrasada)
        assertEquals("2026-10-25", visita.fechaConHora(formatearFecha, formatearHora))
    }

    @Test
    fun fechaSinHora_deHoy_noEstaAtrasadaAunqueSeaDeNoche() {
        val visita = visitaDeObra(obra(hoy), relojA(23 * 60 + 30)) as VisitaDeObra.Programada

        assertEquals(false, visita.atrasada)
    }

    @Test
    fun fechaSinHora_deAyer_estaAtrasada() {
        val visita = visitaDeObra(obra(hoy.minusDays(1)), relojA(8 * 60)) as VisitaDeObra.Programada

        assertEquals(true, visita.atrasada)
    }

    @Test
    fun fechaConHora_elTextoLlevaLaHora() {
        val visita = visitaDeObra(obra(hoy.plusDays(1), hora = 9 * 60 + 5), relojA(10 * 60)) as VisitaDeObra.Programada

        assertEquals(9 * 60 + 5, visita.hora)
        assertEquals(false, visita.atrasada)
        assertEquals("2026-10-23 · 09:05", visita.fechaConHora(formatearFecha, formatearHora))
    }

    @Test
    fun fechaConHora_deHoy_vencePorLaHoraNoPorElDia() {
        val antes = visitaDeObra(obra(hoy, hora = 15 * 60), relojA(14 * 60)) as VisitaDeObra.Programada
        val despues = visitaDeObra(obra(hoy, hora = 15 * 60), relojA(16 * 60)) as VisitaDeObra.Programada

        assertEquals(false, antes.atrasada)
        assertEquals(true, despues.atrasada)
    }

    @Test
    fun horaFueraDeRango_seTrataComoSinHora() {
        val visita = visitaDeObra(obra(hoy.plusDays(1), hora = 5000), relojA(10 * 60)) as VisitaDeObra.Programada

        assertEquals(null, visita.hora)
        assertEquals("2026-10-23", visita.fechaConHora(formatearFecha, formatearHora))
    }

    @Test
    fun relojFijoSeLeeEnLaZonaLocal() {
        // 02:00 UTC del 22 es todavía el 21 en Buenos Aires: una visita del 21 sin hora no está atrasada.
        val reloj = Clock.fixed(Instant.parse("2026-10-22T02:00:00Z"), zona)
        val visita = visitaDeObra(obra(LocalDate.of(2026, 10, 21)), reloj) as VisitaDeObra.Programada

        assertEquals(false, visita.atrasada)
    }
}
