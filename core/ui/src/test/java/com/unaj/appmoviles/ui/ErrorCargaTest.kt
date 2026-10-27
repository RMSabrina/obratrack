package com.unaj.appmoviles.ui

import com.unaj.appmoviles.obratrack.core.domain.error.CausaError
import com.unaj.appmoviles.obratrack.core.ui.components.mensajeDeCarga
import org.junit.Assert.assertEquals
import org.junit.Test

class ErrorCargaTest {

    @Test
    fun cadaCausaTieneUnMensajeDistinto() {
        val mensajes = CausaError.entries.map { it.mensajeDeCarga() }

        assertEquals(CausaError.entries.size, mensajes.toSet().size)
    }
}
