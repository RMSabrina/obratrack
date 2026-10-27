package com.unaj.appmoviles.features.home.agenda

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun AgendaRoute(
    viewModel: AgendaViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToObras: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onOpenObra: (String) -> Unit,
    onIniciarRelevamiento: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    AgendaScreen(
        state = state,
        onBack = onBack,
        onNavigateToHome = onNavigateToHome,
        onNavigateToObras = onNavigateToObras,
        onNavigateToProfile = onNavigateToProfile,
        onOpenObra = onOpenObra,
        onIniciarRelevamiento = onIniciarRelevamiento,
        onFiltroChanged = viewModel::onFiltroChanged,
        onIniciarReprogramacion = viewModel::onIniciarReprogramacion,
        onCancelarReprogramacion = viewModel::onCancelarReprogramacion,
        onConfirmarReprogramacion = viewModel::onConfirmarReprogramacion,
        onClearFeedbackMessage = viewModel::onClearFeedbackMessage,
        onRetry = viewModel::reintentar,
        modifier = modifier
    )
}
