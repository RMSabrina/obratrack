package com.unaj.appmoviles.obratrack.notification

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * Obra que pidió abrir una notificación y todavía no se abrió. Vive en la Activity y en el
 * SavedStateHandle: sobrevive a la rotación y al cierre del proceso (por ejemplo, durante el login).
 */
@HiltViewModel
class AperturaPendienteViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    val obraPendiente: StateFlow<String?> = savedStateHandle.getStateFlow(KEY_OBRA_PENDIENTE, null)

    fun recibir(action: String?, data: String?, lanzadoDesdeHistorial: Boolean) {
        val obraId = obraIdDeIntent(action, data, lanzadoDesdeHistorial) ?: return
        savedStateHandle[KEY_OBRA_PENDIENTE] = obraId
    }

    /** Se llama en el mismo paso que la navegación: cada intent navega una sola vez. */
    fun consumir() {
        savedStateHandle[KEY_OBRA_PENDIENTE] = null
    }

    /** Al cerrar sesión a mano, la obra pendiente ya no corresponde. */
    fun descartar() {
        savedStateHandle[KEY_OBRA_PENDIENTE] = null
    }

    private companion object {
        const val KEY_OBRA_PENDIENTE = "obra_pendiente"
    }
}
