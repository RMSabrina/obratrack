package com.unaj.appmoviles.features.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.unaj.appmoviles.obratrack.core.domain.model.Relevamiento

import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun HomeRoute(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToObras: () -> Unit,
    onNavigateToObraSelection: () -> Unit,
    onNavigateToObrasPublicas: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onOpenRelevamiento: (Relevamiento) -> Unit,
    onTriggerDemoNotification: (() -> Unit)? = null
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    
    HomeScreen(
        state = state,
        onNavigateToObras = onNavigateToObras,
        onNavigateToObraSelection = onNavigateToObraSelection,
        onNavigateToObrasPublicas = onNavigateToObrasPublicas,
        onNavigateToProfile = onNavigateToProfile,
        onOpenRelevamiento = onOpenRelevamiento,
        onTriggerDemoNotification = onTriggerDemoNotification
    )
}