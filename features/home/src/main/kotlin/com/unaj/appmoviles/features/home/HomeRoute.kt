package com.unaj.appmoviles.features.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.unaj.appmoviles.obratrack.core.domain.model.Relevamiento

@Composable
fun HomeRoute(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToObras: () -> Unit,
    onNavigateToAgenda: () -> Unit,
    onNavigateToObraSelection: () -> Unit,
    onNavigateToObrasPublicas: () -> Unit,
    onOpenRelevamiento: (Relevamiento) -> Unit,
    onIniciarRelevamientoParaObra: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    
    HomeScreen(
        state = state,
        onNavigateToObras = onNavigateToObras,
        onNavigateToAgenda = onNavigateToAgenda,
        onNavigateToObraSelection = onNavigateToObraSelection,
        onNavigateToObrasPublicas = onNavigateToObrasPublicas,
        onOpenRelevamiento = onOpenRelevamiento,
        onIniciarRelevamientoParaObra = onIniciarRelevamientoParaObra,
        onRetry = viewModel::reintentar
    )
}