package com.unaj.appmoviles.obratrack.core.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ModoTemaTest {

    @Test
    fun desdeNombre_reconoceLosTresModos() {
        assertEquals(ModoTema.SISTEMA, ModoTema.desdeNombre("SISTEMA"))
        assertEquals(ModoTema.CLARO, ModoTema.desdeNombre("CLARO"))
        assertEquals(ModoTema.OSCURO, ModoTema.desdeNombre("OSCURO"))
    }

    @Test
    fun desdeNombre_ausenteODesconocido_siguenAlSistema() {
        assertEquals(ModoTema.SISTEMA, ModoTema.desdeNombre(null))
        assertEquals(ModoTema.SISTEMA, ModoTema.desdeNombre(""))
        assertEquals(ModoTema.SISTEMA, ModoTema.desdeNombre("oscuro"))
        assertEquals(ModoTema.SISTEMA, ModoTema.desdeNombre("AUTO"))
    }

    @Test
    fun ida_yVuelta_porNombre() {
        ModoTema.entries.forEach { assertEquals(it, ModoTema.desdeNombre(it.name)) }
    }

    @Test
    fun elPrimerValorEsElDeLaPrimeraVez() {
        // Quien no eligió nada sigue al sistema; si se reordena el enum, este orden documenta la intención.
        assertEquals(ModoTema.SISTEMA, ModoTema.entries.first())
    }
}
