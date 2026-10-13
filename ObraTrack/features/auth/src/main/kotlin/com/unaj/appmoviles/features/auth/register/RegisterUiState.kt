package com.unaj.appmoviles.features.auth.register

import com.unaj.appmoviles.obratrack.core.domain.error.DomainError

sealed interface RegisterUiState {
    data object Idle : RegisterUiState
    data object Loading : RegisterUiState
    data class Error(val error: DomainError) : RegisterUiState

    data class VerificationSent(
        val email: String,
        val isResending: Boolean = false,
        val resendSent: Boolean = false,
        val resendError: DomainError? = null
    ) : RegisterUiState
}