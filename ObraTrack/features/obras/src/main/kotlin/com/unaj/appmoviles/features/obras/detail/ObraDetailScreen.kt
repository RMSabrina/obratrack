package com.unaj.appmoviles.features.obras.detail

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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.net.Uri
import coil.compose.AsyncImage
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.unaj.appmoviles.features.obras.formatFecha
import com.unaj.appmoviles.features.obras.label
import com.unaj.appmoviles.obratrack.core.domain.model.Obra
import com.unaj.appmoviles.obratrack.core.domain.model.Relevamiento
import com.unaj.appmoviles.obratrack.core.domain.ui.LoadingContent
import com.unaj.appmoviles.obratrack.core.domain.ui.asMessage

@Composable
fun ObraDetailRoute(
    viewModel: ObraDetailViewModel,
    onBack: () -> Unit,
    onEditObra: (Obra) -> Unit,
    onCreateRelevamiento: () -> Unit,
    onEditRelevamiento: (Relevamiento) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ObraDetailScreen(
        state = state,
        onBack = onBack,
        onEditObra = onEditObra,
        onCreateRelevamiento = onCreateRelevamiento,
        onEditRelevamiento = onEditRelevamiento
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ObraDetailScreen(
    state: ObraDetailUiState,
    onBack: () -> Unit,
    onEditObra: (Obra) -> Unit,
    onCreateRelevamiento: () -> Unit,
    onEditRelevamiento: (Relevamiento) -> Unit,
    modifier: Modifier = Modifier
) {
    val obra = state.obra
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(obra?.nombre ?: "Obra") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Volver") }
                },
                actions = {
                    if (obra != null) {
                        TextButton(onClick = { onEditObra(obra) }) { Text("Editar") }
                    }
                }
            )
        },
        floatingActionButton = {
            if (obra != null) {
                FloatingActionButton(onClick = onCreateRelevamiento) { Text("+") }
            }
        }
    ) { padding ->
        when {
            state.isLoading -> LoadingContent(Modifier.padding(padding))
            obra == null -> {
                Text(
                    text = state.error?.asMessage() ?: "No encontramos esa obra",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(padding).padding(24.dp)
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(obra.cliente, style = MaterialTheme.typography.titleMedium)
                            Text(obra.direccion)
                            Text(
                                "${obra.estado.label()} · ${
                                    obra.ultimaVisita?.let { "Última visita ${formatFecha(it)}" } ?: "Sin visitas"
                                }",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    item { HorizontalDivider() }
                    item { Text("Relevamientos", style = MaterialTheme.typography.titleMedium) }
                    if (state.relevamientos.isEmpty()) {
                        item { Text("Todavía no hay relevamientos.") }
                    } else {
                        items(state.relevamientos, key = { it.id }) { relevamiento ->
                            RelevamientoCard(
                                relevamiento = relevamiento,
                                onClick = { onEditRelevamiento(relevamiento) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RelevamientoCard(relevamiento: Relevamiento, onClick: () -> Unit) {
    val context = LocalContext.current
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(formatFecha(relevamiento.fecha), style = MaterialTheme.typography.labelLarge)
            relevamiento.fotoUrl?.let { url ->
                AsyncImage(
                    model = url,
                    contentDescription = "Foto del relevamiento",
                    modifier = Modifier.fillMaxWidth().height(180.dp)
                )
            }
            Text(relevamiento.nota, maxLines = 3)
            relevamiento.ubicacion?.let { ubicacion ->
                TextButton(onClick = {
                    val geo = Uri.parse(
                        "geo:${ubicacion.latitud},${ubicacion.longitud}?q=${ubicacion.latitud},${ubicacion.longitud}"
                    )
                    context.startActivity(Intent(Intent.ACTION_VIEW, geo))
                }) { Text("Ver en el mapa") }
            }
        }
    }
}
