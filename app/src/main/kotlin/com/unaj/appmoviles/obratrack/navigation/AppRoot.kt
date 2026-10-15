package com.unaj.appmoviles.obratrack.navigation

import AppContainer
import SessionViewModel
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.unaj.appmoviles.obratrack.core.domain.ui.LoadingContent


@Composable
fun AppRoot(container: AppContainer) {
    val sessionViewModel: SessionViewModel = viewModel(factory = container.sessionViewModelFactory)
    val session by sessionViewModel.uiState.collectAsStateWithLifecycle()

    when (session) {
        SessionUiState.Loading -> LoadingContent()
        SessionUiState.Unauthenticated -> AuthNavigation(container)
        SessionUiState.Authenticated -> AppShell(container, onLogout = sessionViewModel::logout)
    }
}