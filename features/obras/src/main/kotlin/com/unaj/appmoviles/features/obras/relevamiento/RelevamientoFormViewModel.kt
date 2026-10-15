package com.unaj.appmoviles.features.obras.relevamiento

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unaj.appmoviles.obratrack.core.domain.error.AppResult
import com.unaj.appmoviles.obratrack.core.domain.error.RelevamientoError
import com.unaj.appmoviles.obratrack.core.domain.model.RelevamientoDraft
import com.unaj.appmoviles.obratrack.core.domain.usecase.auth.ObserveSessionUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.relevamientos.CreateRelevamientoUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.relevamientos.UpdateRelevamientoUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class RelevamientoFormViewModel(
    private val observeSession: ObserveSessionUseCase,
    private val createRelevamiento: CreateRelevamientoUseCase,
    private val updateRelevamiento: UpdateRelevamientoUseCase,
    private val obraId: String,
    private val relevamientoId: String?
) : ViewModel() {

    private val _uiState = MutableStateFlow<RelevamientoFormUiState>(RelevamientoFormUiState.Idle)
    val uiState: StateFlow<RelevamientoFormUiState> = _uiState.asStateFlow()

    fun save(draft: RelevamientoDraft, photo: ByteArray?) {
        if (_uiState.value is RelevamientoFormUiState.Loading) return
        
        // Validación de tamaño de imagen (límite de 5 MB)
        val maxSizeInBytes = 5 * 1024 * 1024
        if (photo != null && photo.size > maxSizeInBytes) {
            _uiState.value = RelevamientoFormUiState.Error(RelevamientoError.ImagenMuyGrande)
            return
        }
        
        _uiState.value = RelevamientoFormUiState.Loading
        viewModelScope.launch {
            val user = observeSession().first()
            if (user == null) {
                _uiState.value = RelevamientoFormUiState.Error(RelevamientoError.NoAutorizado)
                return@launch
            }
            val result = if (relevamientoId == null) {
                createRelevamiento(user.id, obraId, draft, photo)
            } else {
                updateRelevamiento(user.id, obraId, relevamientoId, draft, photo)
            }
            _uiState.value = when (result) {
                is AppResult.Success -> RelevamientoFormUiState.Saved
                is AppResult.Error -> RelevamientoFormUiState.Error(result.error)
            }
        }
    }

    fun onErrorShown() {
        if (_uiState.value is RelevamientoFormUiState.Error) {
            _uiState.value = RelevamientoFormUiState.Idle
        }
    }
}
