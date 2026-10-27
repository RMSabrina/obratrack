package com.unaj.appmoviles.features.obras

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.InputStream

class FotoUiTest {

    private fun bytes(n: Int) = ByteArray(n) { (it % 251).toByte() }

    @Test
    fun leerConLimite_archivoChico_devuelveTodo() {
        val datos = bytes(1_000)

        assertArrayEquals(datos, ByteArrayInputStream(datos).leerConLimite(limite = 5_000))
    }

    @Test
    fun leerConLimite_conTamanoConocido_leeExacto() {
        val datos = bytes(3_000)

        assertArrayEquals(datos, ByteArrayInputStream(datos).leerConLimite(limite = 5_000, tamanoConocido = 3_000))
    }

    @Test
    fun leerConLimite_archivoEnorme_noLeeMasDeLimiteMasUno() {
        val enorme = ByteArrayInputStream(bytes(100_000))

        val leido = enorme.leerConLimite(limite = 20_000)

        assertEquals(20_001, leido.size)
    }

    @Test
    fun leerConLimite_tamanoConocidoMuyGrande_igualCorta() {
        val enorme = ByteArrayInputStream(bytes(100_000))

        val leido = enorme.leerConLimite(limite = 20_000, tamanoConocido = 100_000)

        assertEquals(20_001, leido.size)
    }

    @Test
    fun leerConLimite_tamanoConocidoMayorAlReal_devuelveLoLeido() {
        val datos = bytes(500)

        assertArrayEquals(datos, ByteArrayInputStream(datos).leerConLimite(limite = 5_000, tamanoConocido = 800))
    }

    @Test
    fun leerConLimite_streamQueEntregaDeAPoco_juntaTodo() {
        val datos = bytes(10_000)
        val gotaAGota = object : InputStream() {
            private val origen = ByteArrayInputStream(datos)
            override fun read(): Int = origen.read()
            override fun read(b: ByteArray, off: Int, len: Int): Int = origen.read(b, off, minOf(len, 7))
        }

        assertArrayEquals(datos, gotaAGota.leerConLimite(limite = 50_000, tamanoConocido = 10_000))
    }
}
