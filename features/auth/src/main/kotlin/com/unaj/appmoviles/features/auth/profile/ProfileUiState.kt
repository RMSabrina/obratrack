package com.unaj.appmoviles.features.auth.profile

import androidx.annotation.StringRes
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
    /** Idioma elegido en la app; null = "idioma del sistema". */
    val selectedLanguage: com.unaj.appmoviles.obratrack.core.domain.model.AppLanguage? = null,
    val selectedTema: com.unaj.appmoviles.obratrack.core.domain.model.ModoTema =
        com.unaj.appmoviles.obratrack.core.domain.model.ModoTema.SISTEMA,
    val selectedAnticipacion: com.unaj.appmoviles.obratrack.core.domain.model.AnticipacionAviso =
        com.unaj.appmoviles.obratrack.core.domain.model.AnticipacionAviso.POR_DEFECTO,
    @StringRes val successMessage: Int? = null,
    val error: DomainError? = null
)
