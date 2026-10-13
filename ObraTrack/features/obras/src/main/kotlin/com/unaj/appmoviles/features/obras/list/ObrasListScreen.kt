package com.unaj.appmoviles.features.obras.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.unaj.appmoviles.features.obras.formatFecha
import com.unaj.appmoviles.features.obras.label
import com.unaj.appmoviles.obratrack.core.domain.model.Obra
import com.unaj.appmoviles.obratrack.core.domain.ui.LoadingContent
import com.unaj.appmoviles.obratrack.core.domain.ui.asMessage

@Composable
fun ObrasListRoute(
    viewModel: ObrasListViewModel,
    onCreate: () -> Unit,
    onOpen: (Obra) -> Unit,
    onLogout: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ObrasListScreen(
        state = state,
        onCreate = onCreate,
        onOpen = onOpen,
        onLogout = onLogout
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ObrasListScreen(
    state: ObrasListUiState,
    onCreate: () -> Unit,
    onOpen: (Obra) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Mis obras") },
                actions = {
                    TextButton(onClick = onLogout) { Text("Cerrar sesión") }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreate) { Text("+") }
        }
    ) { padding ->
        when {
            state.isLoading -> LoadingContent(Modifier.padding(padding))
            state.error != null && state.obras.isEmpty() -> {
                Text(
                    text = state.error.asMessage(),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(padding).padding(24.dp)
                )
            }
            state.obras.isEmpty() -> {
                Text(
                    text = "Todavía no cargaste obras.",
                    modifier = Modifier.padding(padding).padding(24.dp)
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.obras, key = { it.id }) { obra ->
                        ObraCard(obra = obra, onClick = { onOpen(obra) })
                    }
                }
            }
        }
    }
}

@Composable
private fun ObraCard(obra: Obra, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(obra.nombre, style = MaterialTheme.typography.titleMedium)
            Text(obra.cliente, style = MaterialTheme.typography.bodyMedium)
            Text(
                "${obra.estado.label()} · ${formatUltimaVisita(obra.ultimaVisita)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatUltimaVisita(millis: Long?): String {
    if (millis == null) return "Sin visitas"
    return "Última visita ${formatFecha(millis)}"
}
