package com.unaj.appmoviles.features.auth.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unaj.appmoviles.obratrack.core.domain.error.AppResult
import com.unaj.appmoviles.obratrack.core.domain.error.AuthError
import com.unaj.appmoviles.obratrack.core.domain.usecase.auth.ChangePasswordUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.auth.DeleteAccountUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.auth.ObserveSessionUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.auth.ReauthenticateUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.auth.UpdateProfileNameUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import dagger.hilt.android.lifecycle.HiltViewModel
import com.unaj.appmoviles.auth.R
import com.unaj.appmoviles.obratrack.core.domain.model.AnticipacionAviso
import com.unaj.appmoviles.obratrack.core.domain.model.AppLanguage
import com.unaj.appmoviles.obratrack.core.domain.repository.AvisoPreferencesRepository
import com.unaj.appmoviles.obratrack.core.domain.repository.LanguagePreferencesRepository
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    observeSessionUseCase: ObserveSessionUseCase,
    private val updateProfileNameUseCase: UpdateProfileNameUseCase,
    private val changePasswordUseCase: ChangePasswordUseCase,
    private val deleteAccountUseCase: DeleteAccountUseCase,
    private val reauthenticateUseCase: ReauthenticateUseCase,
    private val languagePreferencesRepository: LanguagePreferencesRepository,
    private val avisoPreferencesRepository: AvisoPreferencesRepository
) : ViewModel() {

    private val internalState = MutableStateFlow(ProfileUiState())

    val uiState: StateFlow<ProfileUiState> = combine(
        observeSessionUseCase(),
        languagePreferencesRepository.observeLanguage(),
        avisoPreferencesRepository.observeAnticipacion(),
        internalState
    ) { sessionUser, language, anticipacion, state ->
        val nameInputToUse = if (state.nameInput.isEmpty() && sessionUser != null) {
            sessionUser.name
        } else {
            state.nameInput
        }
        state.copy(
            user = sessionUser,
            nameInput = nameInputToUse,
            selectedLanguage = language,
            selectedAnticipacion = anticipacion,
            isLoading = sessionUser == null && state.error == null
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ProfileUiState(isLoading = true)
    )

    fun onLanguageChange(language: AppLanguage) {
        viewModelScope.launch {
            languagePreferencesRepository.setLanguage(language)
        }
    }

    fun onAnticipacionChange(anticipacion: AnticipacionAviso) {
        viewModelScope.launch {
            avisoPreferencesRepository.setAnticipacion(anticipacion)
        }
    }

    fun onNameChange(newName: String) {
        internalState.value = internalState.value.copy(nameInput = newName, error = null)
    }

    fun saveName() {
        val currentInput = uiState.value.nameInput
        if (uiState.value.isSavingName) return

        viewModelScope.launch {
            internalState.value = internalState.value.copy(
                isSavingName = true,
                error = null,
                successMessage = null
            )

            when (val result = updateProfileNameUseCase(currentInput)) {
                is AppResult.Success -> {
                    internalState.value = internalState.value.copy(
                        isSavingName = false,
                        successMessage = R.string.auth_nombre_actualizado,
                        nameInput = result.data.name
                    )
                }
                is AppResult.Error -> {
                    internalState.value = internalState.value.copy(
                        isSavingName = false,
                        error = result.error
                    )
                }
            }
        }
    }

    fun showChangePasswordDialog() {
        internalState.value = internalState.value.copy(
            showChangePasswordDialog = true,
            error = null
        )
    }

    fun hideChangePasswordDialog() {
        internalState.value = internalState.value.copy(showChangePasswordDialog = false)
    }

    fun changePassword(newPassword: String) {
        viewModelScope.launch {
            internalState.value = internalState.value.copy(
                isChangingPassword = true,
                error = null,
                successMessage = null
            )

            when (val result = changePasswordUseCase(newPassword)) {
                is AppResult.Success -> {
                    internalState.value = internalState.value.copy(
                        isChangingPassword = false,
                        showChangePasswordDialog = false,
                        successMessage = R.string.auth_password_actualizada
                    )
                }
                is AppResult.Error -> {
                    if (result.error == AuthError.RequiresRecentLogin) {
                        internalState.value = internalState.value.copy(
                            isChangingPassword = false,
                            showChangePasswordDialog = false,
                            showReAuthDialog = true,
                            pendingAction = PendingAction.CHANGE_PASSWORD
                        )
                    } else {
                        internalState.value = internalState.value.copy(
                            isChangingPassword = false,
                            error = result.error
                        )
                    }
                }
            }
        }
    }

    fun showDeleteAccountDialog() {
        internalState.value = internalState.value.copy(
            showDeleteAccountDialog = true,
            error = null
        )
    }

    fun hideDeleteAccountDialog() {
        internalState.value = internalState.value.copy(showDeleteAccountDialog = false)
    }

    fun confirmDeleteAccount() {
        viewModelScope.launch {
            internalState.value = internalState.value.copy(
                isDeletingAccount = true,
                error = null,
                successMessage = null
            )

            when (val result = deleteAccountUseCase()) {
                is AppResult.Success -> {
                    internalState.value = internalState.value.copy(
                        isDeletingAccount = false,
                        showDeleteAccountDialog = false,
                        successMessage = R.string.auth_cuenta_eliminada
                    )
                }
                is AppResult.Error -> {
                    if (result.error == AuthError.RequiresRecentLogin) {
                        internalState.value = internalState.value.copy(
                            isDeletingAccount = false,
                            showDeleteAccountDialog = false,
                            showReAuthDialog = true,
                            pendingAction = PendingAction.DELETE_ACCOUNT
                        )
                    } else {
                        internalState.value = internalState.value.copy(
                            isDeletingAccount = false,
                            error = result.error
                        )
                    }
                }
            }
        }
    }

    fun hideReAuthDialog() {
        internalState.value = internalState.value.copy(
            showReAuthDialog = false,
            pendingAction = PendingAction.NONE
        )
    }

    fun reauthenticateAndRetry(currentPassword: String) {
        viewModelScope.launch {
            internalState.value = internalState.value.copy(
                isReauthenticating = true,
                error = null
            )

            when (val result = reauthenticateUseCase(currentPassword)) {
                is AppResult.Success -> {
                    val pending = internalState.value.pendingAction
                    internalState.value = internalState.value.copy(
                        isReauthenticating = false,
                        showReAuthDialog = false,
                        pendingAction = PendingAction.NONE
                    )
                    when (pending) {
                        PendingAction.CHANGE_PASSWORD -> {
                            showChangePasswordDialog()
                        }
                        PendingAction.DELETE_ACCOUNT -> {
                            confirmDeleteAccount()
                        }
                        PendingAction.NONE -> {}
                    }
                }
                is AppResult.Error -> {
                    internalState.value = internalState.value.copy(
                        isReauthenticating = false,
                        error = result.error
                    )
                }
            }
        }
    }

    fun clearMessage() {
        internalState.value = internalState.value.copy(successMessage = null)
    }

    fun clearError() {
        internalState.value = internalState.value.copy(error = null)
    }
}
