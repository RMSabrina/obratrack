package com.unaj.appmoviles.features.obras.detail

import com.unaj.appmoviles.obratrack.core.domain.model.Relevamiento
import com.unaj.appmoviles.obratrack.core.domain.model.ResumenTareas
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ObraDetailTabTest {

    private fun relevamiento(id: String, fecha: Long) = Relevamiento(
        id = id,
        obraId = "obra-1",
        userId = "dueno",
        fecha = fecha,
        nota = ""
    )

    // Pestañas visibles

    @Test
    fun pestanasVisibles_dueno_veLasCuatroEnOrden() {
        assertEquals(
            listOf(
                ObraDetailTab.RESUMEN,
                ObraDetailTab.TAREAS,
                ObraDetailTab.CRONOGRAMA,
                ObraDetailTab.RELEVAMIENTOS
            ),
            pestanasVisibles(esDueno = true)
        )
    }

    @Test
    fun pestanasVisibles_noDueno_noVeTareasNiCronograma() {
        assertEquals(
            listOf(ObraDetailTab.RESUMEN, ObraDetailTab.RELEVAMIENTOS),
            pestanasVisibles(esDueno = false)
        )
    }

    // FAB por pestaña

    @Test
    fun fab_segunLaPestana() {
        assertEquals(FabDetalle.NUEVO_RELEVAMIENTO, ObraDetailTab.RESUMEN.fab())
        assertEquals(FabDetalle.NUEVA_TAREA, ObraDetailTab.TAREAS.fab())
        assertNull(ObraDetailTab.CRONOGRAMA.fab())
        assertEquals(FabDetalle.NUEVO_RELEVAMIENTO, ObraDetailTab.RELEVAMIENTOS.fab())
    }

    // Insignia de atrasadas

    @Test
    fun insigniaAtrasadas_conAtrasadas_devuelveLaCantidad() {
        assertEquals(2, insigniaAtrasadas(ResumenTareas(completadas = 1, pendientes = 4, atrasadas = 2)))
    }

    @Test
    fun insigniaAtrasadas_sinAtrasadas_esNull() {
        assertNull(insigniaAtrasadas(ResumenTareas(completadas = 1, pendientes = 4, atrasadas = 0)))
    }

    @Test
    fun insigniaAtrasadas_sinResumen_esNull() {
        assertNull(insigniaAtrasadas(null))
    }

    // Relevamientos recientes del Resumen

    @Test
    fun recientes_conMenosDeTres_devuelveTodos() {
        val lista = listOf(relevamiento("a", 2), relevamiento("b", 1))

        assertEquals(lista, lista.recientes())
    }

    @Test
    fun recientes_conTresExactos_devuelveLosTres() {
        val lista = listOf(relevamiento("a", 3), relevamiento("b", 2), relevamiento("c", 1))

        assertEquals(lista, lista.recientes())
    }

    @Test
    fun recientes_conMasDeTres_devuelveLosPrimerosTresEnElMismoOrden() {
        val lista = (5 downTo 1).map { relevamiento("r$it", it.toLong()) }

        assertEquals(listOf("r5", "r4", "r3"), lista.recientes().map { it.id })
    }

    @Test
    fun recientes_sinRelevamientos_listaVacia() {
        assertEquals(emptyList<Relevamiento>(), emptyList<Relevamiento>().recientes())
    }
}
