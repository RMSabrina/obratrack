package com.unaj.appmoviles.obratrack.navigation

import androidx.navigation3.runtime.NavKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavegacionPilaTest {

    private fun pila(vararg destinos: NavKey): MutableList<NavKey> = destinos.toMutableList()

    @Test
    fun repetirLaMismaPantallaConLosMismosArgumentos_noApila() {
        val pila = pila(HomeDestination, ObraDetailDestination("A"))

        val aplico = pila.navegarA(ObraDetailDestination("A"))

        assertFalse(aplico)
        assertEquals(listOf(HomeDestination, ObraDetailDestination("A")), pila)
    }

    @Test
    fun mismaPantallaConOtroArgumento_apila() {
        val pila = pila(HomeDestination, ObraDetailDestination("A"))

        val aplico = pila.navegarA(ObraDetailDestination("B"))

        assertTrue(aplico)
        assertEquals(
            listOf(HomeDestination, ObraDetailDestination("A"), ObraDetailDestination("B")),
            pila
        )
    }

    @Test
    fun otraPantalla_apila() {
        val pila = pila(HomeDestination)

        assertTrue(pila.navegarA(AgendaDestination))

        assertEquals(listOf(HomeDestination, AgendaDestination), pila)
    }

    @Test
    fun destinoSinArgumentos_repetidoNoApila() {
        val pila = pila(HomeDestination, ObrasDestination)

        assertFalse(pila.navegarA(ObrasDestination))
        assertEquals(2, pila.size)
    }

    @Test
    fun formularioConDatosDistintos_apila() {
        val pila = pila(HomeDestination, RelevamientoFormDestination(obraId = "A"))

        assertTrue(pila.navegarA(RelevamientoFormDestination(obraId = "A", tareaId = "t1")))
        assertFalse(pila.navegarA(RelevamientoFormDestination(obraId = "A", tareaId = "t1")))
        assertEquals(3, pila.size)
    }

    @Test
    fun aVolverAUnaPantallaQueNoEstaArriba_apilaDeNuevo() {
        val pila = pila(HomeDestination, ObraDetailDestination("A"), ObraDetailDestination("B"))

        assertTrue(pila.navegarA(ObraDetailDestination("A")))
        assertEquals(4, pila.size)
    }

    @Test
    fun aperturaDesdeNotificacion_conLaObraYaAbierta_noDuplica_yConOtraObra_apila() {
        // La notificación navega con la misma función que el resto de la app (AppShell).
        val pila = pila(HomeDestination)

        assertTrue(pila.navegarA(ObraDetailDestination("A")))
        assertFalse(pila.navegarA(ObraDetailDestination("A")))
        assertTrue(pila.navegarA(ObraDetailDestination("B")))

        assertEquals(
            listOf(HomeDestination, ObraDetailDestination("A"), ObraDetailDestination("B")),
            pila
        )
    }

    @Test
    fun esDeNivelSuperior_soloInicioObrasAgendaYPerfil() {
        assertTrue(HomeDestination.esDeNivelSuperior())
        assertTrue(ObrasDestination.esDeNivelSuperior())
        assertTrue(AgendaDestination.esDeNivelSuperior())
        assertTrue(ProfileDestination.esDeNivelSuperior())
        assertFalse(ObraDetailDestination("A").esDeNivelSuperior())
        assertFalse(RelevamientoFormDestination(obraId = "A").esDeNivelSuperior())
        assertFalse(ObraSelectionDestination.esDeNivelSuperior())
        assertFalse(ObrasPublicasDestination.esDeNivelSuperior())
    }

    @Test
    fun cambiarDePestana_desdeInicio_apilaLaPestana() {
        val pila = pila(HomeDestination)

        assertTrue(pila.cambiarDePestana(AgendaDestination))

        assertEquals(listOf(HomeDestination, AgendaDestination), pila)
    }

    @Test
    fun cambiarDePestana_entrePestanas_reemplazaLaDeArriba() {
        val pila = pila(HomeDestination, AgendaDestination)

        assertTrue(pila.cambiarDePestana(ObrasDestination))

        assertEquals(listOf(HomeDestination, ObrasDestination), pila)
    }

    @Test
    fun cambiarDePestana_dobleToque_noHaceNada() {
        val pila = pila(HomeDestination, ObrasDestination)

        assertFalse(pila.cambiarDePestana(ObrasDestination))
        assertFalse(pila(HomeDestination).cambiarDePestana(HomeDestination))
        assertEquals(listOf(HomeDestination, ObrasDestination), pila)
    }

    @Test
    fun cambiarDePestana_aInicio_dejaSoloInicio() {
        val pila = pila(HomeDestination, ProfileDestination)

        assertTrue(pila.cambiarDePestana(HomeDestination))

        assertEquals(listOf(HomeDestination), pila)
    }

    @Test
    fun cambiarDePestana_conPantallasEncima_lasDescarta() {
        val pila = pila(HomeDestination, ObrasDestination, ObraDetailDestination("A"))

        assertTrue(pila.cambiarDePestana(ProfileDestination))

        assertEquals(listOf(HomeDestination, ProfileDestination), pila)
    }

    @Test
    fun cambiarDePestana_despuesDeAbrirUnaObraDesdeNotificacion_siguePudiendoNavegar() {
        val pila = pila(HomeDestination)
        assertTrue(pila.navegarA(ObraDetailDestination("A")))

        assertTrue(pila.cambiarDePestana(AgendaDestination))
        assertTrue(pila.navegarA(ObraDetailDestination("A")))

        assertEquals(listOf(HomeDestination, AgendaDestination, ObraDetailDestination("A")), pila)
    }
}
