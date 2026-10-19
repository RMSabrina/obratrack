package com.unaj.appmoviles.obratrack.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unaj.appmoviles.obratrack.core.domain.usecase.auth.LogoutUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.auth.ObserveSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SessionUiState {
    data object Loading : SessionUiState
    data object Authenticated : SessionUiState
    data object Unauthenticated : SessionUiState
}

@HiltViewModel
class SessionViewModel @Inject constructor(
    observeSession: ObserveSessionUseCase,
    private val logoutUseCase: LogoutUseCase
) : ViewModel() {

    val uiState: StateFlow<SessionUiState> = observeSession()
        .map { if (it != null) SessionUiState.Authenticated else SessionUiState.Unauthenticated }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SessionUiState.Loading)

    fun logout() {
        viewModelScope.launch { logoutUseCase() }
    }
}
