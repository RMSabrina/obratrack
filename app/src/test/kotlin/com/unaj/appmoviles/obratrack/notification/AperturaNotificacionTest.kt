package com.unaj.appmoviles.obratrack.notification

import com.unaj.appmoviles.obratrack.navigation.SessionUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AperturaNotificacionTest {

    private val obraId = "aB3_x-9Qz"

    @Test
    fun intentDeObra_devuelveElId() {
        assertEquals(obraId, obraIdDeIntent(ACTION_ABRIR_OBRA, uriObra(obraId), lanzadoDesdeHistorial = false))
    }

    @Test
    fun intentSinAccionOSinDato_seIgnora() {
        // Abrir la app desde el ícono: acción MAIN, sin dato.
        assertNull(obraIdDeIntent("android.intent.action.MAIN", null, lanzadoDesdeHistorial = false))
        assertNull(obraIdDeIntent(null, uriObra(obraId), lanzadoDesdeHistorial = false))
        assertNull(obraIdDeIntent(ACTION_ABRIR_OBRA, null, lanzadoDesdeHistorial = false))
        assertNull(obraIdDeIntent(ACTION_ABRIR_OBRA, "otra://obra/$obraId", lanzadoDesdeHistorial = false))
    }

    @Test
    fun intentAbiertoDesdeRecientes_seIgnora() {
        assertNull(obraIdDeIntent(ACTION_ABRIR_OBRA, uriObra(obraId), lanzadoDesdeHistorial = true))
    }

    @Test
    fun obraIdInvalido_seIgnora() {
        assertFalse(esObraIdValido(null))
        assertFalse(esObraIdValido(""))
        assertFalse(esObraIdValido("a".repeat(129)))
        assertFalse(esObraIdValido("obra/../otra"))
        assertFalse(esObraIdValido("obra 1"))
        assertFalse(esObraIdValido("obra?x=1"))
        assertFalse(esObraIdValido("obrá"))
        assertNull(obraIdDeIntent(ACTION_ABRIR_OBRA, uriObra(""), lanzadoDesdeHistorial = false))
        assertNull(obraIdDeIntent(ACTION_ABRIR_OBRA, uriObra("a/b"), lanzadoDesdeHistorial = false))
    }

    @Test
    fun obraIdValido_seAcepta() {
        assertTrue(esObraIdValido("a".repeat(128)))
        assertTrue(esObraIdValido("Kx7_Ab-20CharsFirest"))
    }

    @Test
    fun decision_segunSesion() {
        assertEquals(DecisionApertura.Esperar, decidirApertura(obraId, SessionUiState.Loading))
        assertEquals(DecisionApertura.PedirLogin, decidirApertura(obraId, SessionUiState.Unauthenticated))
        assertEquals(DecisionApertura.AbrirObra(obraId), decidirApertura(obraId, SessionUiState.Authenticated))
    }

    @Test
    fun sinObraPendiente_noHaceNada() {
        assertEquals(DecisionApertura.Nada, decidirApertura(null, SessionUiState.Loading))
        assertEquals(DecisionApertura.Nada, decidirApertura(null, SessionUiState.Unauthenticated))
        assertEquals(DecisionApertura.Nada, decidirApertura(null, SessionUiState.Authenticated))
    }
}
