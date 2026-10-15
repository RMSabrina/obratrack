package com.unaj.appmoviles.features.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.unaj.appmoviles.obratrack.core.domain.model.Relevamiento

@Composable
fun HomeRoute(
    viewModel: HomeViewModel,
    onNavigateToObras: () -> Unit,
    onNavigateToObraSelection: () -> Unit,
    onOpenRelevamiento: (Relevamiento) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    
    HomeScreen(
        state = state,
        onNavigateToObras = onNavigateToObras,
        onNavigateToObraSelection = onNavigateToObraSelection,
        onOpenRelevamiento = onOpenRelevamiento
    )
}