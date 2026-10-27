package com.unaj.appmoviles.features.obras.list

import com.unaj.appmoviles.obratrack.core.domain.model.EstadoObra
import com.unaj.appmoviles.obratrack.core.domain.model.Obra
import org.junit.Assert.assertEquals
import org.junit.Test

class PublicWorksFilterTest {

    private val obras = listOf(
        obra("1", "Pavimentación de Av. San Martín", "Municipio de Quilmes", "Av. San Martín 1200", EstadoObra.ACTIVA),
        obra("2", "Plaza del barrio", "Gobierno local", "Calle Mitre 44", EstadoObra.FINALIZADA),
        obra("3", "Centro de salud", "Municipio de Quilmes", "Calle Belgrano 90", EstadoObra.PAUSADA)
    )

    @Test
    fun buscaPorNombreClienteODireccion_sinDistinguirMayusculasNiAcentos() {
        assertEquals(listOf("1"), filtrarObrasPublicas(obras, "PAVIMENTACION", null).map { it.id })
        assertEquals(listOf("1", "3"), filtrarObrasPublicas(obras, "quilmes", null).map { it.id })
        assertEquals(listOf("2"), filtrarObrasPublicas(obras, "mitre", null).map { it.id })
    }

    @Test
    fun combinaLaBusquedaConElEstadoElegido() {
        assertEquals(
            listOf("1"),
            filtrarObrasPublicas(obras, "quilmes", EstadoObra.ACTIVA).map { it.id }
        )
    }

    @Test
    fun filtroGuardadas_muestraSoloLasObrasDelIndicePersonal() {
        assertEquals(
            listOf("2"),
            filtrarObrasPublicas(obras, "", null, setOf("2"), soloSuscripciones = true).map { it.id }
        )
    }

    private fun obra(id: String, nombre: String, cliente: String, direccion: String, estado: EstadoObra) = Obra(
        id = id,
        userId = "admin",
        nombre = nombre,
        cliente = cliente,
        direccion = direccion,
        publica = true,
        estado = estado,
        fechaInicio = 0L,
        ultimaVisita = null
    )
}
