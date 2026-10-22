package com.unaj.appmoviles.obratrack.core.domain.usecase

import com.unaj.appmoviles.obratrack.core.domain.model.EstadoTarea
import com.unaj.appmoviles.obratrack.core.domain.model.ResumenTareas
import com.unaj.appmoviles.obratrack.core.domain.model.Tarea
import com.unaj.appmoviles.obratrack.core.domain.usecase.tareas.estaAtrasada
import com.unaj.appmoviles.obratrack.core.domain.usecase.tareas.estaPlanificada
import com.unaj.appmoviles.obratrack.core.domain.usecase.tareas.ordenadas
import com.unaj.appmoviles.obratrack.core.domain.usecase.tareas.resumen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class TareaReglasTest {

    private val hoy = LocalDate.of(2026, 10, 22)

    @Test
    fun atrasada_sinFechaFin_nuncaEstaAtrasada() {
        assertFalse(tarea().estaAtrasada(hoy))
    }

    @Test
    fun atrasada_finPasado_yNoCompletada_estaAtrasada() {
        val tarea = tarea(fechaInicio = hoy.minusDays(5), fechaFin = hoy.minusDays(1))
        assertTrue(tarea.estaAtrasada(hoy))
    }

    @Test
    fun atrasada_finPasado_enCurso_estaAtrasada() {
        val tarea = tarea(
            fechaInicio = hoy.minusDays(5),
            fechaFin = hoy.minusDays(1),
            estado = EstadoTarea.EN_CURSO
        )
        assertTrue(tarea.estaAtrasada(hoy))
    }

    @Test
    fun atrasada_finHoy_noEstaAtrasada() {
        val tarea = tarea(fechaInicio = hoy.minusDays(5), fechaFin = hoy)
        assertFalse(tarea.estaAtrasada(hoy))
    }

    @Test
    fun atrasada_finPasado_peroCompletada_noEstaAtrasada() {
        val tarea = tarea(
            fechaInicio = hoy.minusDays(5),
            fechaFin = hoy.minusDays(1),
            estado = EstadoTarea.COMPLETADA
        )
        assertFalse(tarea.estaAtrasada(hoy))
    }

    @Test
    fun planificada_conAmbasFechas_estaPlanificada() {
        assertTrue(tarea(fechaInicio = hoy, fechaFin = hoy.plusDays(3)).estaPlanificada())
    }

    @Test
    fun planificada_sinFechas_noEstaPlanificada() {
        assertFalse(tarea().estaPlanificada())
    }

    @Test
    fun planificada_conUnaSolaFecha_noEstaPlanificada() {
        assertFalse(tarea(fechaInicio = hoy).estaPlanificada())
        assertFalse(tarea(fechaFin = hoy).estaPlanificada())
    }

    @Test
    fun orden_porFechaDeInicio_sinFechaAlFinal() {
        val sinFecha = tarea(id = "sin", titulo = "A sin fecha")
        val tarde = tarea(id = "tarde", fechaInicio = hoy.plusDays(5), fechaFin = hoy.plusDays(6))
        val temprano = tarea(id = "temprano", fechaInicio = hoy, fechaFin = hoy.plusDays(1))

        val ids = listOf(sinFecha, tarde, temprano).ordenadas().map { it.id }

        assertEquals(listOf("temprano", "tarde", "sin"), ids)
    }

    @Test
    fun orden_mismaFecha_desempataPorTitulo() {
        val b = tarea(id = "b", titulo = "beta", fechaInicio = hoy, fechaFin = hoy)
        val a = tarea(id = "a", titulo = "Alfa", fechaInicio = hoy, fechaFin = hoy)

        val ids = listOf(b, a).ordenadas().map { it.id }

        assertEquals(listOf("a", "b"), ids)
    }

    @Test
    fun resumen_listaVacia_todoEnCero() {
        assertEquals(ResumenTareas(0, 0, 0), emptyList<Tarea>().resumen(hoy))
    }

    @Test
    fun resumen_atrasadasCuentanTambienComoPendientes() {
        val tareas = listOf(
            tarea(estado = EstadoTarea.COMPLETADA),
            tarea(estado = EstadoTarea.PENDIENTE),
            tarea(estado = EstadoTarea.EN_CURSO),
            tarea(fechaInicio = hoy.minusDays(5), fechaFin = hoy.minusDays(1)),
            tarea(fechaInicio = hoy.minusDays(5), fechaFin = hoy.minusDays(1), estado = EstadoTarea.EN_CURSO)
        )

        assertEquals(ResumenTareas(completadas = 1, pendientes = 4, atrasadas = 2), tareas.resumen(hoy))
    }

    @Test
    fun resumen_sinFechas_esPendienteYNoAtrasada() {
        assertEquals(ResumenTareas(0, 1, 0), listOf(tarea()).resumen(hoy))
    }

    @Test
    fun resumen_completadaVencida_noEsAtrasada() {
        val completada = tarea(
            fechaInicio = hoy.minusDays(10),
            fechaFin = hoy.minusDays(3),
            estado = EstadoTarea.COMPLETADA
        )

        assertEquals(ResumenTareas(1, 0, 0), listOf(completada).resumen(hoy))
    }

    @Test
    fun resumen_finHoy_esPendienteNoAtrasada() {
        val finHoy = tarea(fechaInicio = hoy.minusDays(2), fechaFin = hoy)

        assertEquals(ResumenTareas(0, 1, 0), listOf(finHoy).resumen(hoy))
    }

    private fun tarea(
        id: String = "t",
        titulo: String = "Tarea",
        fechaInicio: LocalDate? = null,
        fechaFin: LocalDate? = null,
        estado: EstadoTarea = EstadoTarea.PENDIENTE
    ) = Tarea(
        id = id,
        obraId = "obra-1",
        userId = "uid",
        titulo = titulo,
        fechaInicio = fechaInicio,
        fechaFin = fechaFin,
        estado = estado
    )
}
