package com.unaj.appmoviles.features.obras.relevamiento

import com.unaj.appmoviles.obratrack.core.domain.model.EstadoTarea
import com.unaj.appmoviles.obratrack.core.domain.model.Tarea
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class OpcionesSelectorTest {

    private fun tarea(
        id: String,
        titulo: String,
        inicio: LocalDate? = null,
        fin: LocalDate? = inicio,
        estado: EstadoTarea = EstadoTarea.PENDIENTE
    ) = Tarea(id = id, obraId = "o1", userId = "u1", titulo = titulo, fechaInicio = inicio, fechaFin = fin, estado = estado)

    @Test
    fun titulosUnicos_noSeMarcanComoRepetidos() {
        val opciones = listOf(tarea("a", "Cimientos"), tarea("b", "Pintura")).opcionesSelector()

        assertTrue(opciones.none { it.repetida })
        assertEquals(listOf(0, 0), opciones.map { it.numero })
    }

    @Test
    fun titulosIguales_sePuedenDistinguirPorNumeroYFechas() {
        val opciones = listOf(
            tarea("a", "Colar losa", LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 5)),
            tarea("b", "Colar losa", LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 5))
        ).opcionesSelector()

        assertTrue(opciones.all { it.repetida })
        assertEquals(setOf(1, 2), opciones.map { it.numero }.toSet())
        assertEquals(2, opciones.map { it.fechaInicio }.toSet().size)
    }

    @Test
    fun ignoraMayusculasYEspaciosAlComparar() {
        val opciones = listOf(tarea("a", "Pintura"), tarea("b", " pintura ")).opcionesSelector()

        assertTrue(opciones.all { it.repetida })
    }

    @Test
    fun sinFechas_igualSeNumeran() {
        val opciones = listOf(tarea("a", "Limpieza"), tarea("b", "Limpieza"), tarea("c", "Otra")).opcionesSelector()

        val limpiezas = opciones.filter { it.titulo == "Limpieza" }
        assertEquals(setOf(1, 2), limpiezas.map { it.numero }.toSet())
        assertEquals(listOf(null, null), limpiezas.map { it.fechaInicio })
        assertFalse(opciones.single { it.titulo == "Otra" }.repetida)
    }

    @Test
    fun conservaElEstadoCompletada() {
        val opciones = listOf(
            tarea("a", "Techo", estado = EstadoTarea.COMPLETADA),
            tarea("b", "Techo")
        ).opcionesSelector()

        assertEquals(1, opciones.count { it.completada })
    }
}
