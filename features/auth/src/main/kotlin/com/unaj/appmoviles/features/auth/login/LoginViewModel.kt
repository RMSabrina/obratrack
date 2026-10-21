package com.unaj.appmoviles.features.auth.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unaj.appmoviles.obratrack.core.domain.error.AppResult
import com.unaj.appmoviles.obratrack.core.domain.error.AuthError
import com.unaj.appmoviles.obratrack.core.domain.model.Credentials
import com.unaj.appmoviles.obratrack.core.domain.usecase.auth.LoginUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.auth.LoginWithGoogleUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val login: LoginUseCase,
    private val loginWithGoogle: LoginWithGoogleUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onLoginClick(email: String, password: String) {
        if (_uiState.value is LoginUiState.Loading) return
        _uiState.value = LoginUiState.Loading
        viewModelScope.launch {
            _uiState.value = when (val result = login(Credentials(email.trim(), password))) {
                is AppResult.Success -> LoginUiState.Idle // la sesión se encarga de navegar
                is AppResult.Error -> LoginUiState.Error(result.error)
            }
        }
    }

    fun onGoogleLoginClick(idToken: String) {
        if (_uiState.value is LoginUiState.Loading) return
        _uiState.value = LoginUiState.Loading
        viewModelScope.launch {
            _uiState.value = when (val result = loginWithGoogle(idToken)) {
                is AppResult.Success -> LoginUiState.Idle
                is AppResult.Error -> LoginUiState.Error(result.error)
            }
        }
    }

    fun onGoogleLoginError(error: AuthError) {
        _uiState.value = LoginUiState.Error(error)
    }

    fun onErrorShown() {
        if (_uiState.value is LoginUiState.Error) _uiState.value = LoginUiState.Idle
    }
}
