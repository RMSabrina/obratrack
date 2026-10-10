package com.unaj.appmoviles.features.auth.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unaj.appmoviles.obratrack.core.domain.error.AppResult
import com.unaj.appmoviles.obratrack.core.domain.model.Credentials
import com.unaj.appmoviles.obratrack.core.domain.usecase.RegisterUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


class RegisterViewModel(private val register: RegisterUseCase) : ViewModel() {

    private val _uiState = MutableStateFlow<RegisterUiState>(RegisterUiState.Idle)
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun onRegisterClick(name: String, email: String, password: String) {
        if (_uiState.value is RegisterUiState.Loading) return
        _uiState.value = RegisterUiState.Loading
        viewModelScope.launch {
            _uiState.value = when (val result = register(
                name.trim(),
                Credentials(email.trim(), password)
            )) {
                is AppResult.Success -> RegisterUiState.Idle
                is AppResult.Error -> RegisterUiState.Error(result.error)
            }
        }
    }

    fun onErrorShown() {
        if (_uiState.value is RegisterUiState.Error) _uiState.value = RegisterUiState.Idle
    }
}
