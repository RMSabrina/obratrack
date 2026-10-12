package com.unaj.appmoviles.features.obras.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unaj.appmoviles.obratrack.core.domain.error.ObraError
import com.unaj.appmoviles.obratrack.core.domain.usecase.ObserveObrasUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.ObserveSessionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ObrasListViewModel(
    observeSession: ObserveSessionUseCase,
    private val observeObras: ObserveObrasUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ObrasListUiState())
    val uiState: StateFlow<ObrasListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeSession().collectLatest { user ->
                if (user == null) {
                    _uiState.value = ObrasListUiState(isLoading = false, error = ObraError.NoAutorizada)
                    return@collectLatest
                }
                observeObras(user.id)
                    .catch {
                        _uiState.value = ObrasListUiState(isLoading = false, error = ObraError.Desconocido)
                    }
                    .collect { obras ->
                        _uiState.value = ObrasListUiState(isLoading = false, obras = obras)
                    }
            }
        }
    }
}
