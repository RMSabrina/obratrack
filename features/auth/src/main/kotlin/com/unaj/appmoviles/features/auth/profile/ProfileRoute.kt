package com.unaj.appmoviles.features.auth.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun ProfileRoute(
    viewModel: ProfileViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onLogout: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ProfileScreen(
        state = state,
        onNameChange = viewModel::onNameChange,
        onSaveName = viewModel::saveName,
        onShowChangePasswordDialog = viewModel::showChangePasswordDialog,
        onHideChangePasswordDialog = viewModel::hideChangePasswordDialog,
        onChangePassword = viewModel::changePassword,
        onShowDeleteAccountDialog = viewModel::showDeleteAccountDialog,
        onHideDeleteAccountDialog = viewModel::hideDeleteAccountDialog,
        onConfirmDeleteAccount = viewModel::confirmDeleteAccount,
        onHideReAuthDialog = viewModel::hideReAuthDialog,
        onReauthenticateAndRetry = viewModel::reauthenticateAndRetry,
        onClearMessage = viewModel::clearMessage,
        onClearError = viewModel::clearError,
        onLanguageChange = viewModel::onLanguageChange,
        onAnticipacionChange = viewModel::onAnticipacionChange,
        onBack = onBack,
        onLogout = onLogout
    )
}
