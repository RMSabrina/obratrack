package com.unaj.appmoviles.features.auth.login

import com.unaj.appmoviles.obratrack.core.domain.error.DomainError

sealed interface LoginUiState {
    data object Idle : LoginUiState
    data object Loading : LoginUiState
    data class Error(val error: DomainError) : LoginUiState
}