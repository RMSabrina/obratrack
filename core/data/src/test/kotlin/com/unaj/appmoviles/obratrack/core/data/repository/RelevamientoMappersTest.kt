package com.unaj.appmoviles.obratrack.core.data.repository

import com.unaj.appmoviles.obratrack.core.domain.model.RelevamientoDraft
import com.unaj.appmoviles.obratrack.core.firebase.RelevamientoDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RelevamientoMappersTest {

    private fun dto(tareaId: String? = null) = RelevamientoDto(
        id = "r1",
        obraId = "obra-1",
        userId = "uid",
        fechaMillis = 1_700_000_000_000L,
        nota = "Avance",
        tareaId = tareaId
    )

    @Test
    fun relevamientoAnteriorSinTareaId_seLeeSinVinculoYConservaElResto() {
        // Así llega un documento viejo: el data source lee el campo ausente como null.
        val relevamiento = RelevamientoDto(
            id = "r1",
            obraId = "obra-1",
            userId = "uid",
            fechaMillis = 1_700_000_000_000L,
            nota = "Avance"
        ).toDomain()

        assertNull(relevamiento.tareaId)
        assertEquals("r1", relevamiento.id)
        assertEquals("Avance", relevamiento.nota)
        assertEquals(1_700_000_000_000L, relevamiento.fecha)
    }

    @Test
    fun tareaIdVacio_seLeeSinVinculo() {
        assertNull(dto(tareaId = "").toDomain().tareaId)
        assertNull(dto(tareaId = "  ").toDomain().tareaId)
    }

    @Test
    fun tareaIdValido_seConserva() {
        assertEquals("t1", dto(tareaId = "t1").toDomain().tareaId)
    }

    @Test
    fun toDto_llevaLaTareaElegida_oNullSinTarea() {
        val conTarea = RelevamientoDraft(fecha = 1L, nota = "n", tareaId = "t1").toDto("uid", "obra-1")
        val sinTarea = RelevamientoDraft(fecha = 1L, nota = "n").toDto("uid", "obra-1")

        assertEquals("t1", conTarea.tareaId)
        assertNull(sinTarea.tareaId)
    }
}
