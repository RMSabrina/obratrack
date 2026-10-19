package com.unaj.appmoviles.features.auth.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unaj.appmoviles.obratrack.core.domain.error.AppResult
import com.unaj.appmoviles.obratrack.core.domain.model.Credentials
import com.unaj.appmoviles.obratrack.core.domain.usecase.auth.RegisterUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.auth.ResendVerificationUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val register: RegisterUseCase,
    private val resendVerification: ResendVerificationUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<RegisterUiState>(RegisterUiState.Idle)
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    // Solo en memoria: Firebase exige login para reenviar el correo
    private var pendingPassword: String? = null

    fun onRegisterClick(name: String, email: String, password: String) {
        if (_uiState.value is RegisterUiState.Loading) return
        _uiState.value = RegisterUiState.Loading
        viewModelScope.launch {
            _uiState.value = when (val result = register(
                name.trim(),
                Credentials(email.trim(), password)
            )) {
                is AppResult.Success -> {
                    pendingPassword = password
                    RegisterUiState.VerificationSent(email.trim())
                }
                is AppResult.Error -> RegisterUiState.Error(result.error)
            }
        }
    }

    fun onResendVerification() {
        val current = _uiState.value as? RegisterUiState.VerificationSent ?: return
        val password = pendingPassword ?: return
        if (current.isResending) return

        _uiState.value = current.copy(isResending = true, resendSent = false, resendError = null)
        viewModelScope.launch {
            _uiState.value = when (val result = resendVerification(current.email, password)) {
                is AppResult.Success -> current.copy(isResending = false, resendSent = true)
                is AppResult.Error -> current.copy(isResending = false, resendError = result.error)
            }
        }
    }

    fun onErrorShown() {
        if (_uiState.value is RegisterUiState.Error) _uiState.value = RegisterUiState.Idle
    }

    override fun onCleared() {
        pendingPassword = null
    }
}