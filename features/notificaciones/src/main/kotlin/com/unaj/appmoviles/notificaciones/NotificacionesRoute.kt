package com.unaj.appmoviles.notificaciones

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.unaj.appmoviles.obratrack.core.domain.model.NotificacionItem

@Composable
fun NotificacionesRoute(
    onBack: () -> Unit,
    onNavigateToObra: (String) -> Unit,
    onNavigateToAgenda: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NotificacionesViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    NotificacionesScreen(
        state = state,
        onBack = onBack,
        onFilterSelected = viewModel::onFilterSelected,
        onItemClick = { item: NotificacionItem ->
            viewModel.onMarcarComoLeida(item.id)
            val obraId = item.obraId
            if (!obraId.isNullOrBlank()) {
                onNavigateToObra(obraId)
            } else if (item.rutaDestino == "agenda") {
                onNavigateToAgenda()
            }
        },
        onMarkAllAsRead = viewModel::onMarcarTodasComoLeidas,
        modifier = modifier
    )
}
