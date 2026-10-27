package com.unaj.appmoviles.obratrack.ui.theme

import androidx.appcompat.app.AppCompatDelegate
import com.unaj.appmoviles.obratrack.core.domain.model.ModoTema
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModoTemaUiTest {

    @Test
    fun esOscuro_sistemaSigueAlDispositivo() {
        assertTrue(esOscuro(ModoTema.SISTEMA, sistemaOscuro = true))
        assertFalse(esOscuro(ModoTema.SISTEMA, sistemaOscuro = false))
    }

    @Test
    fun esOscuro_claroYOscuroIgnoranAlDispositivo() {
        // Forzar Claro con el sistema oscuro, y al revés.
        assertFalse(esOscuro(ModoTema.CLARO, sistemaOscuro = true))
        assertFalse(esOscuro(ModoTema.CLARO, sistemaOscuro = false))
        assertTrue(esOscuro(ModoTema.OSCURO, sistemaOscuro = true))
        assertTrue(esOscuro(ModoTema.OSCURO, sistemaOscuro = false))
    }

    @Test
    fun aNightMode_mapeaCadaModoAlDeAppCompat() {
        assertEquals(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, ModoTema.SISTEMA.aNightMode())
        assertEquals(AppCompatDelegate.MODE_NIGHT_NO, ModoTema.CLARO.aNightMode())
        assertEquals(AppCompatDelegate.MODE_NIGHT_YES, ModoTema.OSCURO.aNightMode())
    }
}
