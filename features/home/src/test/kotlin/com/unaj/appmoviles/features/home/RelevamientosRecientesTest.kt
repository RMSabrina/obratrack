package com.unaj.appmoviles.features.home

import com.unaj.appmoviles.obratrack.core.domain.model.EstadoObra
import com.unaj.appmoviles.obratrack.core.domain.model.Obra
import com.unaj.appmoviles.obratrack.core.domain.model.Relevamiento
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RelevamientosRecientesTest {

    private val obra = Obra(
        id = "o1",
        userId = "u1",
        nombre = "Casa Pérez",
        cliente = "Pérez",
        direccion = "Mitre 100",
        publica = false,
        estado = EstadoObra.ACTIVA,
        fechaInicio = 0L,
        ultimaVisita = null
    )

    private fun relevamiento(id: String, fecha: Long, obraId: String = "o1") =
        Relevamiento(id = id, obraId = obraId, userId = "u1", fecha = fecha, nota = "")

    @Test
    fun devuelveLosTresMasRecientes_ordenadosDelMasNuevo() {
        val relevamientos = listOf(
            relevamiento("r1", fecha = 1L),
            relevamiento("r2", fecha = 4L),
            relevamiento("r3", fecha = 2L),
            relevamiento("r4", fecha = 3L)
        )

        val recientes = relevamientosRecientes(relevamientos, listOf(obra))

        assertEquals(listOf("r2", "r4", "r3"), recientes.map { it.relevamiento.id })
    }

    @Test
    fun obraConocida_usaSuNombre() {
        val recientes = relevamientosRecientes(listOf(relevamiento("r1", fecha = 1L)), listOf(obra))

        assertEquals("Casa Pérez", recientes.single().obraNombre)
    }

    @Test
    fun obraDesconocida_dejaElNombreEnNull() {
        val recientes = relevamientosRecientes(
            listOf(relevamiento("r1", fecha = 1L, obraId = "borrada")),
            listOf(obra)
        )

        assertNull(recientes.single().obraNombre)
    }
}
