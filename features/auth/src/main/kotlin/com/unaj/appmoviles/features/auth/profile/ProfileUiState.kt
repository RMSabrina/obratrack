package com.unaj.appmoviles.features.auth.profile

import com.unaj.appmoviles.obratrack.core.domain.error.DomainError
import com.unaj.appmoviles.obratrack.core.domain.model.User

enum class PendingAction {
    NONE,
    CHANGE_PASSWORD,
    DELETE_ACCOUNT
}

data class ProfileUiState(
    val user: User? = null,
    val nameInput: String = "",
    val isLoading: Boolean = true,
    val isSavingName: Boolean = false,
    val isChangingPassword: Boolean = false,
    val isDeletingAccount: Boolean = false,
    val isReauthenticating: Boolean = false,
    val showChangePasswordDialog: Boolean = false,
    val showDeleteAccountDialog: Boolean = false,
    val showReAuthDialog: Boolean = false,
    val pendingAction: PendingAction = PendingAction.NONE,
    val successMessage: String? = null,
    val error: DomainError? = null
)
