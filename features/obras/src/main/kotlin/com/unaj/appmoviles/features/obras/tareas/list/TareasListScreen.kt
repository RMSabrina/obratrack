package com.unaj.appmoviles.features.obras.tareas.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.unaj.appmoviles.features.obras.tareas.list.components.TareaCard
import com.unaj.appmoviles.obras.R
import com.unaj.appmoviles.obratrack.core.domain.model.Tarea
import com.unaj.appmoviles.obratrack.core.domain.ui.LoadingContent
import com.unaj.appmoviles.obratrack.core.domain.ui.asMessage
import com.unaj.appmoviles.obratrack.core.ui.components.EmptyState

@Composable
fun TareasListRoute(
    obraId: String,
    onBack: () -> Unit,
    onCreateTarea: () -> Unit,
    onOpenTarea: (Tarea) -> Unit,
    viewModel: TareasListViewModel = hiltViewModel(
        creationCallback = { factory: TareasListViewModel.Factory -> factory.create(obraId) }
    )
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TareasListScreen(
        state = state,
        onBack = onBack,
        onCreateTarea = onCreateTarea,
        onOpenTarea = onOpenTarea
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TareasListScreen(
    state: TareasListUiState,
    onBack: () -> Unit,
    onCreateTarea: () -> Unit,
    onOpenTarea: (Tarea) -> Unit,
    modifier: Modifier = Modifier
) {
    // Solo se puede crear si se cargó el listado (dueño y sin errores).
    val canCreate = !state.isLoading && state.error == null

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tareas_titulo)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.volver)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (canCreate) {
                FloatingActionButton(onClick = onCreateTarea) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.tareas_agregar)
                    )
                }
            }
        }
    ) { padding ->
        when {
            state.isLoading -> LoadingContent(Modifier.padding(padding))
            state.error != null -> Text(
                text = state.error.asMessage(),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .padding(padding)
                    .padding(24.dp)
            )
            state.tareas.isEmpty() -> EmptyState(
                icon = Icons.Default.Checklist,
                title = stringResource(R.string.tareas_vacio_titulo),
                description = stringResource(R.string.tareas_vacio_descripcion),
                actionLabel = stringResource(R.string.tareas_agregar_primera),
                onActionClick = onCreateTarea,
                modifier = Modifier.padding(padding)
            )
            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.tareas, key = { it.tarea.id }) { item ->
                    TareaCard(
                        item = item,
                        onClick = { onOpenTarea(item.tarea) }
                    )
                }
            }
        }
    }
}
