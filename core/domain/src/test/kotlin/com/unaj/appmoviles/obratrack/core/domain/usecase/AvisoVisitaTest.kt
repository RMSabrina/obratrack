package com.unaj.appmoviles.obratrack.core.domain.usecase

import com.unaj.appmoviles.obratrack.core.domain.model.AnticipacionAviso
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.PlanAviso
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.instanteAviso
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.instanteVisita
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.planificarAviso
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class AvisoVisitaTest {

    private val buenosAires = ZoneId.of("America/Argentina/Buenos_Aires")
    private val auckland = ZoneId.of("Pacific/Auckland")
    private val dia = LocalDate.of(2026, 10, 26)

    // Visita a las 15:00 locales.
    private val tresDeLaTarde = 15 * 60
    private val visitaBA = instanteVisita(dia, tresDeLaTarde, buenosAires) // 18:00Z
    private val visitaAuckland = instanteVisita(dia, tresDeLaTarde, auckland) // 02:00Z

    @Test
    fun porDefecto_esUnaHoraAntes() {
        assertEquals(AnticipacionAviso.HORA_1, AnticipacionAviso.POR_DEFECTO)
    }

    @Test
    fun instanteAviso_restaLaAnticipacionElegida() {
        val visita = Instant.parse("2026-10-26T18:00:00Z")

        assertEquals(visita, instanteAviso(visita, AnticipacionAviso.AL_MOMENTO))
        assertEquals(Instant.parse("2026-10-26T17:45:00Z"), instanteAviso(visita, AnticipacionAviso.MIN_15))
        assertEquals(Instant.parse("2026-10-26T17:00:00Z"), instanteAviso(visita, AnticipacionAviso.HORA_1))
        assertEquals(Instant.parse("2026-10-25T18:00:00Z"), instanteAviso(visita, AnticipacionAviso.DIA_1))
    }

    @Test
    fun conAnticipacionSuficiente_programaTodasLasOpciones_enAmbasZonas() {
        for (visita in listOf(visitaBA, visitaAuckland)) {
            // Dos días antes: ninguna opción ha vencido.
            val ahora = visita.minusSeconds(2 * 24 * 3600L)
            for (opcion in AnticipacionAviso.entries) {
                assertEquals(
                    PlanAviso.Programar(instanteAviso(visita, opcion)),
                    planificarAviso(ahora, visita, opcion)
                )
            }
        }
    }

    @Test
    fun avisoYaVencido_conLaVisitaPorDelante_avisaEnseguida_yNoProgramaEnElPasado() {
        // Faltan 30 minutos: venció "1 hora antes" y "1 día antes", no "15 minutos" ni "al momento".
        val ahora = visitaBA.minusSeconds(30 * 60L)

        assertEquals(PlanAviso.AvisarYa, planificarAviso(ahora, visitaBA, AnticipacionAviso.HORA_1))
        assertEquals(PlanAviso.AvisarYa, planificarAviso(ahora, visitaBA, AnticipacionAviso.DIA_1))
        assertEquals(
            PlanAviso.Programar(visitaBA.minusSeconds(15 * 60L)),
            planificarAviso(ahora, visitaBA, AnticipacionAviso.MIN_15)
        )
        assertEquals(PlanAviso.Programar(visitaBA), planificarAviso(ahora, visitaBA, AnticipacionAviso.AL_MOMENTO))
    }

    @Test
    fun enElInstanteExactoDelAviso_yaNoEsFuturo_avisaEnseguida() {
        val ahora = instanteAviso(visitaBA, AnticipacionAviso.HORA_1)

        assertEquals(PlanAviso.AvisarYa, planificarAviso(ahora, visitaBA, AnticipacionAviso.HORA_1))
    }

    @Test
    fun visitaYaPasada_noSeAvisa() {
        assertEquals(PlanAviso.Ninguno, planificarAviso(visitaBA, visitaBA, AnticipacionAviso.AL_MOMENTO))
        assertEquals(PlanAviso.Ninguno, planificarAviso(visitaBA.plusSeconds(1), visitaBA, AnticipacionAviso.HORA_1))
    }

    @Test
    fun cruceDeDia_visitaDespuesDeMedianoche_avisaEnElDiaAnterior() {
        // Visita a las 00:30 del 26: una hora antes es 23:30 del 25; un día antes, 00:30 del 25.
        val visita = instanteVisita(dia, 30, buenosAires)
        val ahora = visita.minusSeconds(3 * 24 * 3600L)

        val unaHora = (planificarAviso(ahora, visita, AnticipacionAviso.HORA_1) as PlanAviso.Programar).instante
        val unDia = (planificarAviso(ahora, visita, AnticipacionAviso.DIA_1) as PlanAviso.Programar).instante

        assertEquals(LocalDate.of(2026, 10, 25), unaHora.atZone(buenosAires).toLocalDate())
        assertEquals(23 to 30, unaHora.atZone(buenosAires).let { it.hour to it.minute })
        assertEquals(LocalDate.of(2026, 10, 25), unDia.atZone(buenosAires).toLocalDate())
        assertEquals(0 to 30, unDia.atZone(buenosAires).let { it.hour to it.minute })
    }

    @Test
    fun cruceDeDia_enAuckland_elAvisoCaeEnLaFechaLocalDelDiaAnterior() {
        val visita = instanteVisita(dia, 15, auckland) // 00:15 locales del 26
        val ahora = visita.minusSeconds(3 * 24 * 3600L)

        val aviso = (planificarAviso(ahora, visita, AnticipacionAviso.HORA_1) as PlanAviso.Programar).instante

        assertEquals(LocalDate.of(2026, 10, 25), aviso.atZone(auckland).toLocalDate())
        assertEquals(23 to 15, aviso.atZone(auckland).let { it.hour to it.minute })
    }
}
