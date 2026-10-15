package com.unaj.appmoviles.features.auth.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unaj.appmoviles.obratrack.core.domain.error.AppResult
import com.unaj.appmoviles.obratrack.core.domain.usecase.auth.ObserveSessionUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.auth.UpdateProfileNameUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProfileViewModel(
    observeSessionUseCase: ObserveSessionUseCase,
    private val updateProfileNameUseCase: UpdateProfileNameUseCase
) : ViewModel() {

    private val nameInputState = MutableStateFlow<String?>(null)
    private val isSavingState = MutableStateFlow(false)
    private val successMessageState = MutableStateFlow<String?>(null)
    private val errorState = MutableStateFlow<com.unaj.appmoviles.obratrack.core.domain.error.DomainError?>(null)

    val uiState: StateFlow<ProfileUiState> = combine(
        observeSessionUseCase(),
        nameInputState,
        isSavingState,
        successMessageState,
        errorState
    ) { user, nameInput, isSaving, successMessage, error ->
        ProfileUiState(
            user = user,
            nameInput = nameInput ?: user?.name.orEmpty(),
            isLoading = user == null && error == null,
            isSaving = isSaving,
            successMessage = successMessage,
            error = error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ProfileUiState(isLoading = true)
    )

    fun onNameChange(newName: String) {
        nameInputState.value = newName
        errorState.value = null
    }

    fun saveName() {
        val currentInput = uiState.value.nameInput
        if (isSavingState.value) return

        viewModelScope.launch {
            isSavingState.value = true
            errorState.value = null
            successMessageState.value = null

            when (val result = updateProfileNameUseCase(currentInput)) {
                is AppResult.Success -> {
                    successMessageState.value = "Nombre actualizado con éxito"
                    nameInputState.value = result.data.name
                }
                is AppResult.Error -> {
                    errorState.value = result.error
                }
            }
            isSavingState.value = false
        }
    }

    fun clearMessage() {
        successMessageState.value = null
    }

    fun clearError() {
        errorState.value = null
    }
}
