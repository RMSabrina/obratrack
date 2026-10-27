package com.unaj.appmoviles.features.obras

import com.unaj.appmoviles.obratrack.core.domain.model.Ubicacion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ControlCapturaTest {

    private val aqui = Ubicacion(-34.6, -58.4)

    /** Un GPS de mentira: guarda el callback para responder cuando el test quiera. */
    private class GpsFalso {
        var pedidos = 0
        var cancelados = 0
        var responder: ((Ubicacion?) -> Unit)? = null

        fun lanzar(onResult: (Ubicacion?) -> Unit): () -> Unit {
            pedidos++
            responder = onResult
            return { cancelados++ }
        }
    }

    private fun control(gps: GpsFalso, capturando: Boolean = false, recibidos: MutableList<Ubicacion?>) =
        ControlCaptura(capturando, gps::lanzar) { recibidos += it }

    @Test
    fun iniciar_marcaCapturandoYAlLlegarLaUbicacionAvisaYTermina() {
        val gps = GpsFalso()
        val recibidos = mutableListOf<Ubicacion?>()
        val control = control(gps, recibidos = recibidos)

        control.iniciar()
        assertTrue(control.capturando)

        gps.responder!!(aqui)

        assertFalse(control.capturando)
        assertEquals(listOf<Ubicacion?>(aqui), recibidos)
    }

    @Test
    fun iniciar_conUnPedidoEnCurso_noLanzaOtro() {
        val gps = GpsFalso()
        val control = control(gps, recibidos = mutableListOf())

        control.iniciar()
        control.iniciar()

        assertEquals(1, gps.pedidos)
    }

    @Test
    fun cancelar_cortaElPedidoYNoAvisaResultado() {
        val gps = GpsFalso()
        val recibidos = mutableListOf<Ubicacion?>()
        val control = control(gps, recibidos = recibidos)
        control.iniciar()

        control.cancelar()
        // El GPS puede responder igual (la cancelación llega como "sin ubicación"): se ignora.
        gps.responder!!(null)

        assertFalse(control.capturando)
        assertEquals(1, gps.cancelados)
        assertTrue(recibidos.isEmpty())
    }

    @Test
    fun cancelar_yVolverAIniciar_usaElPedidoNuevo() {
        val gps = GpsFalso()
        val recibidos = mutableListOf<Ubicacion?>()
        val control = control(gps, recibidos = recibidos)
        control.iniciar()
        val viejo = gps.responder!!
        control.cancelar()
        control.iniciar()

        viejo(Ubicacion(0.0, 0.0))
        assertTrue(control.capturando)
        assertTrue(recibidos.isEmpty())

        gps.responder!!(aqui)
        assertEquals(listOf<Ubicacion?>(aqui), recibidos)
    }

    @Test
    fun rotar_cortaElPedidoPeroConservaCapturando_yLaPantallaNuevaLoRetoma() {
        val gps = GpsFalso()
        val recibidos = mutableListOf<Ubicacion?>()
        val antes = control(gps, recibidos = recibidos)
        antes.iniciar()

        antes.liberar()
        assertTrue(antes.capturando)
        assertEquals(1, gps.cancelados)

        // La pantalla recreada recibe el estado guardado y retoma el pedido.
        val despues = control(gps, capturando = antes.capturando, recibidos = recibidos)
        despues.retomarSiCorresponde()
        assertEquals(2, gps.pedidos)

        gps.responder!!(aqui)
        assertFalse(despues.capturando)
        assertEquals(listOf<Ubicacion?>(aqui), recibidos)
    }

    @Test
    fun retomarSiCorresponde_sinCaptura_noHaceNada() {
        val gps = GpsFalso()
        val control = control(gps, recibidos = mutableListOf())

        control.retomarSiCorresponde()

        assertEquals(0, gps.pedidos)
    }

    @Test
    fun siElGpsNoEncuentraUbicacion_avisaConNull() {
        val gps = GpsFalso()
        val recibidos = mutableListOf<Ubicacion?>()
        val control = control(gps, recibidos = recibidos)
        control.iniciar()

        gps.responder!!(null)

        assertFalse(control.capturando)
        assertNull(recibidos.single())
    }

    @Test
    fun siElResultadoLlegaAntesDeVolverDeLanzar_noQuedaPedidoColgado() {
        val recibidos = mutableListOf<Ubicacion?>()
        var cancelados = 0
        val control = ControlCaptura(false, { onResult -> onResult(aqui); { cancelados++ } }) { recibidos += it }

        control.iniciar()
        control.cancelar()

        assertEquals(0, cancelados)
        assertEquals(listOf<Ubicacion?>(aqui), recibidos)
    }
}
