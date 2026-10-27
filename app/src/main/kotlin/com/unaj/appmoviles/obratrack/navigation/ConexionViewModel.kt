package com.unaj.appmoviles.obratrack.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unaj.appmoviles.obratrack.core.domain.usecase.sync.ObserveConexionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ConexionViewModel @Inject constructor(
    observeConexion: ObserveConexionUseCase
) : ViewModel() {
    /** Arranca en false: no se muestra el banner hasta confirmar que no hay red. */
    val sinConexion: StateFlow<Boolean> = observeConexion()
        .map { enLinea -> !enLinea }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
}
