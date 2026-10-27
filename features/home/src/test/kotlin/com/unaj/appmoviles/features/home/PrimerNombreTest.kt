package com.unaj.appmoviles.features.home

import org.junit.Assert.assertEquals
import org.junit.Test

class PrimerNombreTest {

    @Test
    fun nombreCompleto_devuelveSoloElPrimero() {
        assertEquals("Sabrina", primerNombre("Sabrina Magali Romero"))
    }

    @Test
    fun unSoloNombre_quedaIgual() {
        assertEquals("Sabrina", primerNombre("Sabrina"))
    }

    @Test
    fun ignoraEspaciosAlrededorYRepetidos() {
        assertEquals("Ana", primerNombre("  Ana   María  "))
    }

    @Test
    fun enBlanco_devuelveVacio_paraUsarElTextoPorDefecto() {
        assertEquals("", primerNombre("   "))
        assertEquals("", primerNombre(""))
    }
}
