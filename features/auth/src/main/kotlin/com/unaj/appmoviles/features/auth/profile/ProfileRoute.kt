package com.unaj.appmoviles.features.auth.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ProfileRoute(
    viewModel: ProfileViewModel,
    onBack: () -> Unit,
    onLogout: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ProfileScreen(
        state = state,
        onNameChange = viewModel::onNameChange,
        onSaveName = viewModel::saveName,
        onClearMessage = viewModel::clearMessage,
        onClearError = viewModel::clearError,
        onBack = onBack,
        onLogout = onLogout
    )
}
