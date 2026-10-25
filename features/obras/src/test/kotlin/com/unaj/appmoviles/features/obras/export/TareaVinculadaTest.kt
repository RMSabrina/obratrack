package com.unaj.appmoviles.features.obras.export

import com.unaj.appmoviles.obratrack.core.domain.model.Tarea
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TareaVinculadaTest {

    private val tareas = listOf(
        Tarea(id = "t1", obraId = "o1", userId = "u1", titulo = "Instalación eléctrica")
    )

    @Test
    fun sinTareaId_omiteLaLinea() {
        assertNull(tareaVinculadaTexto(null, tareas))
        assertNull(tareaVinculadaTexto("", tareas))
    }

    @Test
    fun tareaInexistente_omiteLaLinea() {
        assertNull(tareaVinculadaTexto("borrada", tareas))
    }

    @Test
    fun tareaExistente_muestraSuTitulo() {
        assertEquals("Tarea Vinculada: Instalación eléctrica", tareaVinculadaTexto("t1", tareas))
    }
}
