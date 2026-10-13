package com.unaj.appmoviles.features.obras.relevamiento

import com.unaj.appmoviles.obratrack.core.domain.error.DomainError

sealed interface RelevamientoFormUiState {
    data object Idle : RelevamientoFormUiState
    data object Loading : RelevamientoFormUiState
    data class Error(val error: DomainError) : RelevamientoFormUiState
    data object Saved : RelevamientoFormUiState
}
