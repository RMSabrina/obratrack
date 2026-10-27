package com.unaj.appmoviles.features.obras.list

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unaj.appmoviles.obratrack.core.domain.error.ObraError
import com.unaj.appmoviles.obratrack.core.domain.error.aErrorDeLectura
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.ObserveObrasUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.auth.ObserveSessionUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.sync.ObservePendientesUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ObrasListViewModel @Inject constructor(
    observeSession: ObserveSessionUseCase,
    private val observeObras: ObserveObrasUseCase,
    observePendientes: ObservePendientesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ObrasListUiState())
    val uiState: StateFlow<ObrasListUiState> = _uiState.asStateFlow()

    /** Cada incremento vuelve a leer las obras; solo lo hace el botón "Reintentar". */
    private val reintentos = MutableStateFlow(0)

    init {
        viewModelScope.launch {
            observeSession().flatMapLatest { user ->
                if (user == null) flowOf(emptySet()) else observePendientes.obras(user.id)
            }.collect { ids -> _uiState.update { it.copy(pendientes = ids) } }
        }
        viewModelScope.launch {
            observeSession().combine(reintentos) { user, _ -> user }.collectLatest { user ->
                if (user == null) {
                    _uiState.update { it.copy(isLoading = false, error = ObraError.NoAutorizada) }
                    return@collectLatest
                }
                observeObras(user.id)
                    .catch { e ->
                        Log.e(TAG, "No se pudieron leer las obras", e)
                        _uiState.update { it.copy(isLoading = false, obras = emptyList(), error = e.aErrorDeLectura()) }
                    }
                    .collect { obras ->
                        _uiState.update { it.copy(isLoading = false, obras = obras, error = null) }
                    }
            }
        }
    }

    fun reintentar() {
        _uiState.update { ObrasListUiState(pendientes = it.pendientes) }
        reintentos.update { it + 1 }
    }

    private companion object {
        const val TAG = "ObrasListViewModel"
    }
}
