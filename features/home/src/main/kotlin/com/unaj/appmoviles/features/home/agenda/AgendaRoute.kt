package com.unaj.appmoviles.features.home.agenda

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay

private const val INTERVALO_REFRESCO_MS = 30_000L

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

    // Con la pantalla a la vista, recalcula cada tanto qué visitas ya pasaron de hora.
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner, viewModel) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.refrescarHora()
            while (true) {
                delay(INTERVALO_REFRESCO_MS)
                viewModel.refrescarHora()
            }
        }
    }

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
