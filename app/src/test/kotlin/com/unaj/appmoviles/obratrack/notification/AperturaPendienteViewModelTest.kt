package com.unaj.appmoviles.obratrack.notification

import androidx.lifecycle.SavedStateHandle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AperturaPendienteViewModelTest {

    private val savedStateHandle = SavedStateHandle()
    private val viewModel = AperturaPendienteViewModel(savedStateHandle)

    private fun recibirObra(obraId: String, desdeHistorial: Boolean = false) =
        viewModel.recibir(ACTION_ABRIR_OBRA, uriObra(obraId), desdeHistorial)

    @Test
    fun recibir_dejaLaObraPendiente() {
        recibirObra("o1")

        assertEquals("o1", viewModel.obraPendiente.value)
    }

    @Test
    fun consumir_navegaUnaSolaVez() {
        recibirObra("o1")
        viewModel.consumir()

        assertNull(viewModel.obraPendiente.value)
    }

    @Test
    fun laObraPendienteSobreviveAlRecrearElViewModel() {
        recibirObra("o1")

        // Mismo SavedStateHandle, como al restaurar tras el cierre del proceso.
        assertEquals("o1", AperturaPendienteViewModel(savedStateHandle).obraPendiente.value)
    }

    @Test
    fun intentQueNoEsDeObra_noPisaLaPendiente() {
        recibirObra("o1")
        viewModel.recibir("android.intent.action.MAIN", null, lanzadoDesdeHistorial = false)
        viewModel.recibir(ACTION_ABRIR_OBRA, uriObra("id inválido"), lanzadoDesdeHistorial = false)

        assertEquals("o1", viewModel.obraPendiente.value)
    }

    @Test
    fun intentsIgnorados_noDejanNadaPendiente() {
        recibirObra("o1", desdeHistorial = true)
        viewModel.recibir(ACTION_ABRIR_OBRA, uriObra(""), lanzadoDesdeHistorial = false)

        assertNull(viewModel.obraPendiente.value)
    }

    @Test
    fun segundaNotificacion_reemplazaLaPendiente() {
        recibirObra("o1")
        recibirObra("o2")

        assertEquals("o2", viewModel.obraPendiente.value)
    }

    @Test
    fun cerrarSesion_descartaLaPendiente() {
        recibirObra("o1")
        viewModel.descartar()

        assertNull(viewModel.obraPendiente.value)
    }
}
