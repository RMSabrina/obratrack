package com.unaj.appmoviles.features.obras.tareas.cronograma

import com.unaj.appmoviles.obratrack.core.domain.model.EstadoTarea
import com.unaj.appmoviles.obratrack.core.domain.model.Tarea
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CronogramaCalculoTest {

    private val hoy = LocalDate.of(2026, 10, 23)

    private fun tarea(
        id: String,
        inicio: LocalDate?,
        fin: LocalDate?,
        estado: EstadoTarea = EstadoTarea.PENDIENTE,
        titulo: String = "Tarea $id"
    ) = Tarea(
        id = id,
        obraId = "obra-1",
        userId = "user-1",
        titulo = titulo,
        fechaInicio = inicio,
        fechaFin = fin,
        estado = estado
    )

    // Período visible

    @Test
    fun calcularPeriodo_sinTareasPlanificadas_devuelveNull() {
        val tareas = listOf(tarea("1", null, null))

        assertNull(calcularPeriodo(tareas, hoy))
        assertNull(calcularPeriodo(emptyList(), hoy))
    }

    @Test
    fun calcularPeriodo_hoyDentroDeLasTareas_vaDelPrimerInicioAlUltimoFinConMargen() {
        val tareas = listOf(
            tarea("1", LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 25)),
            tarea("2", LocalDate.of(2026, 10, 20), LocalDate.of(2026, 11, 5))
        )

        val periodo = calcularPeriodo(tareas, hoy, margenDias = 3)

        assertEquals(PeriodoVisible(LocalDate.of(2026, 10, 7), LocalDate.of(2026, 11, 8)), periodo)
    }

    @Test
    fun calcularPeriodo_tareasFuturas_seAmpliaHastaHoy() {
        val tareas = listOf(tarea("1", LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 20)))

        val periodo = calcularPeriodo(tareas, hoy, margenDias = 0)

        assertEquals(PeriodoVisible(hoy, LocalDate.of(2026, 11, 20)), periodo)
    }

    @Test
    fun calcularPeriodo_tareasPasadas_seAmpliaHastaHoy() {
        val tareas = listOf(tarea("1", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 10)))

        val periodo = calcularPeriodo(tareas, hoy, margenDias = 0)

        assertEquals(PeriodoVisible(LocalDate.of(2026, 9, 1), hoy), periodo)
    }

    @Test
    fun calcularPeriodo_ignoraTareasSinFechas() {
        val tareas = listOf(
            tarea("1", hoy, hoy.plusDays(2)),
            tarea("2", null, null)
        )

        assertEquals(PeriodoVisible(hoy, hoy.plusDays(2)), calcularPeriodo(tareas, hoy, margenDias = 0))
    }

    @Test
    fun periodoVisible_dias_incluyeAmbosExtremos() {
        assertEquals(1, PeriodoVisible(hoy, hoy).dias)
        assertEquals(31, PeriodoVisible(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)).dias)
    }

    // Posición y ancho de barras

    @Test
    fun toBarra_calculaDesdeDiaYDuracionConFinIncluido() {
        val periodo = PeriodoVisible(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31))
        val barra = tarea("1", LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 9)).toBarra(periodo, hoy)

        assertEquals(4, barra?.desdeDia)
        assertEquals(5, barra?.duracionDias)
    }

    @Test
    fun toBarra_tareaDeUnSoloDia_duraUnDia() {
        val periodo = PeriodoVisible(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31))

        assertEquals(1, tarea("1", hoy, hoy).toBarra(periodo, hoy)?.duracionDias)
    }

    @Test
    fun toBarra_sinFechas_devuelveNull() {
        val periodo = PeriodoVisible(hoy, hoy)

        assertNull(tarea("1", null, null).toBarra(periodo, hoy))
    }

    @Test
    fun toBarra_marcaAtrasadaSegunFechaDeFinYEstado() {
        val periodo = PeriodoVisible(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31))
        val ayer = hoy.minusDays(1)

        assertTrue(tarea("1", ayer, ayer).toBarra(periodo, hoy)!!.atrasada)
        assertFalse(tarea("2", hoy, hoy).toBarra(periodo, hoy)!!.atrasada)
        assertFalse(tarea("3", ayer, ayer, EstadoTarea.COMPLETADA).toBarra(periodo, hoy)!!.atrasada)
    }

    @Test
    fun anchoBarra_esProporcionalALaDuracion() {
        assertEquals(120f, anchoBarra(duracionDias = 5, anchoDia = 24f, anchoMinimo = 12f), 0.001f)
    }

    @Test
    fun anchoBarra_unSoloDiaConEscalaChica_usaElAnchoMinimo() {
        assertEquals(12f, anchoBarra(duracionDias = 1, anchoDia = 4f, anchoMinimo = 12f), 0.001f)
    }

    // Línea de hoy

    @Test
    fun indiceDe_fechaDentroDelPeriodo_devuelveSuPosicion() {
        val periodo = PeriodoVisible(LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 30))

        assertEquals(0, periodo.indiceDe(LocalDate.of(2026, 10, 20)))
        assertEquals(3, periodo.indiceDe(hoy))
        assertEquals(10, periodo.indiceDe(LocalDate.of(2026, 10, 30)))
    }

    @Test
    fun indiceDe_fechaFueraDelPeriodo_devuelveNull() {
        val periodo = PeriodoVisible(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 10))

        assertNull(periodo.indiceDe(LocalDate.of(2026, 9, 30)))
        assertNull(periodo.indiceDe(LocalDate.of(2026, 10, 11)))
    }

    // Encabezado de semanas

    @Test
    fun iniciosDeSemana_devuelveLosLunesDelPeriodo() {
        // 23/10/2026 es viernes; los lunes del período son el 26/10 y el 2/11.
        val periodo = PeriodoVisible(hoy, LocalDate.of(2026, 11, 3))

        assertEquals(listOf(3, 10), periodo.iniciosDeSemana())
    }

    // Cronograma completo

    @Test
    fun calcularCronograma_soloDibujaPlanificadasOrdenadasYCuentaLasSinFechas() {
        val tareas = listOf(
            tarea("tarde", hoy.plusDays(5), hoy.plusDays(6)),
            tarea("sin", null, null),
            tarea("temprana", hoy.minusDays(2), hoy)
        )

        val cronograma = calcularCronograma(tareas, hoy)

        assertEquals(listOf("temprana", "tarde"), cronograma.barras.map { it.tarea.id })
        assertEquals(1, cronograma.sinFechas)
        // Período: hoy-2-margen .. hoy+6+margen; hoy queda en el índice 2 + margen.
        assertEquals(2 + MARGEN_DIAS, cronograma.hoyDia)
        assertEquals(0 + MARGEN_DIAS, cronograma.barras.first().desdeDia)
    }

    @Test
    fun calcularCronograma_sinTareasPlanificadas_noTienePeriodoNiBarras() {
        val cronograma = calcularCronograma(listOf(tarea("1", null, null), tarea("2", null, null)), hoy)

        assertNull(cronograma.periodo)
        assertTrue(cronograma.barras.isEmpty())
        assertNull(cronograma.hoyDia)
        assertEquals(2, cronograma.sinFechas)
    }

    @Test
    fun calcularCronograma_noModificaLasTareas() {
        val original = tarea("1", hoy.minusDays(10), hoy.minusDays(5), EstadoTarea.EN_CURSO)

        val barra = calcularCronograma(listOf(original), hoy).barras.single()

        assertEquals(original, barra.tarea)
        assertTrue(barra.atrasada)
    }
}
