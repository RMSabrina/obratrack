package com.unaj.appmoviles.features.auth.profile

import com.unaj.appmoviles.obratrack.core.domain.error.DomainError
import com.unaj.appmoviles.obratrack.core.domain.model.User

data class ProfileUiState(
    val user: User? = null,
    val nameInput: String = "",
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val successMessage: String? = null,
    val error: DomainError? = null
)
