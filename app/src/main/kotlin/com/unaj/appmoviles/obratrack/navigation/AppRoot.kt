package com.unaj.appmoviles.obratrack.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.unaj.appmoviles.obratrack.core.domain.ui.LoadingContent


@Composable
fun AppRoot() {
    val sessionViewModel: SessionViewModel = hiltViewModel()
    val session by sessionViewModel.uiState.collectAsStateWithLifecycle()

    when (session) {
        SessionUiState.Loading -> LoadingContent()
        SessionUiState.Unauthenticated -> AuthNavigation()
        SessionUiState.Authenticated -> AppShell(onLogout = sessionViewModel::logout)
    }
}