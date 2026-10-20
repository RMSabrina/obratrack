package com.unaj.appmoviles.features.obras.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unaj.appmoviles.obratrack.core.domain.error.AppResult
import com.unaj.appmoviles.obratrack.core.domain.error.ObraError
import com.unaj.appmoviles.obratrack.core.domain.usecase.auth.ObserveSessionUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.DeleteObraUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.ObserveObraUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.relevamientos.ObserveRelevamientosUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel

@HiltViewModel(assistedFactory = ObraDetailViewModel.Factory::class)
class ObraDetailViewModel @AssistedInject constructor(
    @Assisted private val obraId: String,
    observeSession: ObserveSessionUseCase,
    private val observeObra: ObserveObraUseCase,
    private val observeRelevamientos: ObserveRelevamientosUseCase,
    private val deleteObraUseCase: DeleteObraUseCase
) : ViewModel() {

    @dagger.assisted.AssistedFactory
    interface Factory {
        fun create(obraId: String): ObraDetailViewModel
    }

    private val _uiState = MutableStateFlow(ObraDetailUiState())
    val uiState: StateFlow<ObraDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeSession().collectLatest { user ->
                if (user == null) {
                    _uiState.update { it.copy(isLoading = false, error = ObraError.NoAutorizada) }
                    return@collectLatest
                }
                combine(
                    observeObra(user.id, obraId),
                    observeRelevamientos(user.id, obraId)
                ) { obra, relevamientos ->
                    val current = _uiState.value
                    if (current.isDeleting) {
                        if (obra == null && current.deleteError == ObraError.Timeout) {
                            current.copy(isDeleted = true)
                        } else {
                            current
                        }
                    } else {
                        ObraDetailUiState(
                            isLoading = false,
                            isEditable = obra?.userId == user.id,
                            obra = obra,
                            relevamientos = relevamientos,
                            error = if (obra == null) ObraError.NoEncontrada else null,
                            isDeleting = current.isDeleting,
                            isDeleted = current.isDeleted,
                            deleteError = current.deleteError
                        )
                    }
                }.catch {
                    if (!_uiState.value.isDeleting) {
                        _uiState.update { it.copy(isLoading = false, error = ObraError.Desconocido) }
                    }
                }.collect { newState ->
                    _uiState.value = newState
                }
            }
        }
    }

    fun deleteObra() {
        if (_uiState.value.isDeleting) return
        _uiState.update { it.copy(isDeleting = true, deleteError = null) }
        viewModelScope.launch {
            when (val result = deleteObraUseCase(obraId)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(isDeleting = true, isDeleted = true) }
                }
                is AppResult.Error -> {
                    if (result.error == ObraError.Timeout) {
                        if (_uiState.value.obra == null) {
                            _uiState.update { it.copy(isDeleting = true, isDeleted = true) }
                        } else {
                            _uiState.update { it.copy(isDeleting = false, deleteError = ObraError.Timeout) }
                        }
                    } else {
                        _uiState.update { it.copy(isDeleting = false, deleteError = result.error) }
                    }
                }
            }
        }
    }

    fun onDeleteErrorDismissed() {
        _uiState.update { it.copy(deleteError = null) }
    }
}
