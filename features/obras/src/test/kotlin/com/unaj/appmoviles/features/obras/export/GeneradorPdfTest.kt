package com.unaj.appmoviles.features.obras.export

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GeneradorPdfTest {

    // No backgroundScope: advanceUntilIdle() no corre tareas que solo son de fondo.
    private fun TestScope.generador() = GeneradorPdf<String>(this)

    @Test
    fun alEmpezarEstaInactivo() = runTest {
        assertEquals(EstadoPdf.Inactivo, generador().estado.value)
    }

    @Test
    fun mientrasGeneraEstaGenerando_yAlTerminarEstaListo() = runTest {
        val generador = generador()
        val espera = CompletableDeferred<String>()

        generador.generar { espera.await() }
        advanceUntilIdle()
        assertEquals(EstadoPdf.Generando, generador.estado.value)

        espera.complete("informe.pdf")
        advanceUntilIdle()
        assertEquals(EstadoPdf.Listo("informe.pdf"), generador.estado.value)
    }

    @Test
    fun dosToquesSeguidosGeneranUnSoloPdf() = runTest {
        val generador = generador()
        val espera = CompletableDeferred<String>()
        var llamadas = 0

        generador.generar { llamadas++; espera.await() }
        generador.generar { llamadas++; espera.await() }
        espera.complete("informe.pdf")
        advanceUntilIdle()

        assertEquals(1, llamadas)
    }

    @Test
    fun sinResultadoEsError() = runTest {
        val generador = generador()

        generador.generar { null }
        advanceUntilIdle()

        assertEquals(EstadoPdf.Error, generador.estado.value)
    }

    @Test
    fun siLaGeneracionLanzaUnaExcepcionEsErrorYNoSeCae() = runTest {
        val generador = generador()

        generador.generar { throw IllegalStateException("sin espacio") }
        advanceUntilIdle()

        assertEquals(EstadoPdf.Error, generador.estado.value)
    }

    @Test
    fun trasUnErrorSePuedeReintentar() = runTest {
        val generador = generador()
        generador.generar { null }
        advanceUntilIdle()

        generador.consumido()
        assertEquals(EstadoPdf.Inactivo, generador.estado.value)

        generador.generar { "informe.pdf" }
        advanceUntilIdle()
        assertEquals(EstadoPdf.Listo("informe.pdf"), generador.estado.value)
    }

    @Test
    fun consumidoNoPisaUnaGeneracionEnCurso() = runTest {
        val generador = generador()
        val espera = CompletableDeferred<String>()
        generador.generar { espera.await() }
        advanceUntilIdle()

        generador.consumido()

        assertEquals(EstadoPdf.Generando, generador.estado.value)
        espera.complete("informe.pdf")
    }
}
