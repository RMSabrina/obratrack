package com.unaj.appmoviles.features.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.unaj.appmoviles.home.R
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.unaj.appmoviles.features.obras.list.ObrasListUiState
import com.unaj.appmoviles.features.obras.list.ObrasListViewModel
import com.unaj.appmoviles.features.obras.list.components.ObraCard
import com.unaj.appmoviles.obratrack.core.domain.model.Obra
import com.unaj.appmoviles.obratrack.core.domain.ui.LoadingContent
import com.unaj.appmoviles.obratrack.core.ui.components.ErrorCarga

import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun ObraSelectionRoute(
    viewModel: ObrasListViewModel = hiltViewModel(),
    onObraSelected: (Obra) -> Unit,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    
    ObraSelectionScreen(
        state = state,
        onObraSelected = onObraSelected,
        onBack = onBack,
        onRetry = viewModel::reintentar
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ObraSelectionScreen(
    state: ObrasListUiState,
    onObraSelected: (Obra) -> Unit,
    onBack: () -> Unit,
    onRetry: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = stringResource(R.string.obra_seleccion_titulo), 
                        fontWeight = FontWeight.Bold 
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = stringResource(R.string.volver)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Text(
                text = stringResource(R.string.obra_seleccion_instruccion),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = Color.Gray
            )

            val error = state.error
            if (state.isLoading) {
                LoadingContent(Modifier.weight(1f))
            } else if (error != null) {
                ErrorCarga(error = error, onRetry = onRetry, modifier = Modifier.weight(1f))
            } else if (state.obras.isEmpty()) {
                Text(
                    text = stringResource(R.string.obra_seleccion_vacia),
                    modifier = Modifier.padding(24.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(state.obras, key = { it.id }) { obra ->
                        ObraCard(obra = obra, onClick = { onObraSelected(obra) })
                    }
                }
            }
        }
    }
}
