package com.unaj.appmoviles.features.obras.form

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unaj.appmoviles.obratrack.core.domain.error.AppResult
import com.unaj.appmoviles.obratrack.core.domain.error.ObraError
import com.unaj.appmoviles.obratrack.core.domain.model.ObraDraft
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.CreateObraUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.auth.ObserveSessionUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.UpdateObraUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ObraFormViewModel(
    private val observeSession: ObserveSessionUseCase,
    private val createObra: CreateObraUseCase,
    private val updateObra: UpdateObraUseCase,
    private val obraId: String?
) : ViewModel() {

    private val _uiState = MutableStateFlow<ObraFormUiState>(ObraFormUiState.Idle)
    val uiState: StateFlow<ObraFormUiState> = _uiState.asStateFlow()

    fun save(draft: ObraDraft) {
        if (_uiState.value is ObraFormUiState.Loading) return
        _uiState.value = ObraFormUiState.Loading
        viewModelScope.launch {
            val user = observeSession().first()
            if (user == null) {
                _uiState.value = ObraFormUiState.Error(ObraError.NoAutorizada)
                return@launch
            }
            val result = if (obraId == null) {
                createObra(user.id, draft)
            } else {
                updateObra(user.id, obraId, draft)
            }
            _uiState.value = when (result) {
                is AppResult.Success -> ObraFormUiState.Saved
                is AppResult.Error -> ObraFormUiState.Error(result.error)
            }
        }
    }

    fun onErrorShown() {
        if (_uiState.value is ObraFormUiState.Error) _uiState.value = ObraFormUiState.Idle
    }
}
