package com.unaj.appmoviles.features.obras.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unaj.appmoviles.obratrack.core.domain.error.ObraError
import com.unaj.appmoviles.obratrack.core.domain.usecase.auth.ObserveSessionUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.ObserveObraUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.relevamientos.ObserveRelevamientosUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class ObraDetailViewModel(
    private val obraId: String,
    observeSession: ObserveSessionUseCase,
    private val observeObra: ObserveObraUseCase,
    private val observeRelevamientos: ObserveRelevamientosUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ObraDetailUiState())
    val uiState: StateFlow<ObraDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeSession().collectLatest { user ->
                if (user == null) {
                    _uiState.value = ObraDetailUiState(isLoading = false, error = ObraError.NoAutorizada)
                    return@collectLatest
                }
                combine(
                    observeObra(user.id, obraId),
                    observeRelevamientos(user.id, obraId)
                ) { obra, relevamientos ->
                    ObraDetailUiState(
                        isLoading = false,
                        obra = obra,
                        relevamientos = relevamientos,
                        error = if (obra == null) ObraError.NoEncontrada else null
                    )
                }.catch {
                    _uiState.value = ObraDetailUiState(isLoading = false, error = ObraError.Desconocido)
                }.collect { _uiState.value = it }
            }
        }
    }
}
