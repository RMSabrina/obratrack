package com.unaj.appmoviles.features.obras.tareas.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.unaj.appmoviles.features.obras.tareas.list.components.TareaCard
import com.unaj.appmoviles.obras.R
import com.unaj.appmoviles.obratrack.core.domain.model.Tarea
import com.unaj.appmoviles.obratrack.core.domain.ui.LoadingContent
import com.unaj.appmoviles.obratrack.core.ui.components.ErrorCarga
import com.unaj.appmoviles.obratrack.core.ui.components.EmptyState

/** Contenido de la pestaña Tareas del detalle de obra (el Scaffold y el FAB son del detalle). */
@Composable
fun TareasListRoute(
    obraId: String,
    onCreateTarea: () -> Unit,
    onOpenTarea: (Tarea) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TareasListViewModel = hiltViewModel(
        creationCallback = { factory: TareasListViewModel.Factory -> factory.create(obraId) }
    )
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TareasListContent(
        state = state,
        onCreateTarea = onCreateTarea,
        onOpenTarea = onOpenTarea,
        onRetry = viewModel::reintentar,
        modifier = modifier
    )
}

@Composable
fun TareasListContent(
    state: TareasListUiState,
    onCreateTarea: () -> Unit,
    onOpenTarea: (Tarea) -> Unit,
    onRetry: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    when {
        state.isLoading -> LoadingContent(modifier)
        state.error != null -> ErrorCarga(error = state.error, onRetry = onRetry, modifier = modifier)
        state.tareas.isEmpty() -> EmptyState(
            icon = Icons.Default.Checklist,
            title = stringResource(R.string.tareas_vacio_titulo),
            description = stringResource(R.string.tareas_vacio_descripcion),
            actionLabel = stringResource(R.string.tareas_agregar_primera),
            onActionClick = onCreateTarea,
            modifier = modifier
        )
        else -> LazyColumn(
            modifier = modifier.fillMaxSize(),
            // Espacio abajo para que el FAB del detalle no tape la última tarjeta.
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(state.tareas, key = { it.tarea.id }) { item ->
                TareaCard(
                    item = item,
                    onClick = { onOpenTarea(item.tarea) },
                    pendiente = item.tarea.id in state.pendientes
                )
            }
        }
    }
}
