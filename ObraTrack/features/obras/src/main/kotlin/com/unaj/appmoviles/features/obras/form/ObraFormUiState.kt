package com.unaj.appmoviles.features.obras.form

import com.unaj.appmoviles.obratrack.core.domain.error.DomainError

sealed interface ObraFormUiState {
    data object Idle : ObraFormUiState
    data object Loading : ObraFormUiState
    data class Error(val error: DomainError) : ObraFormUiState
    data object Saved : ObraFormUiState
}
