package com.unaj.appmoviles.features.obras.tareas.cronograma

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.unaj.appmoviles.obras.R
import com.unaj.appmoviles.obratrack.core.domain.ui.LoadingContent
import com.unaj.appmoviles.obratrack.core.domain.ui.asMessage
import com.unaj.appmoviles.obratrack.core.ui.components.EmptyState

@Composable
fun CronogramaRoute(
    obraId: String,
    onBack: () -> Unit,
    onOpenTarea: (tareaId: String) -> Unit,
    onOpenTareas: () -> Unit,
    viewModel: CronogramaViewModel = hiltViewModel(
        creationCallback = { factory: CronogramaViewModel.Factory -> factory.create(obraId) }
    )
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    CronogramaScreen(
        state = state,
        onBack = onBack,
        onOpenTarea = onOpenTarea,
        onOpenTareas = onOpenTareas
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CronogramaScreen(
    state: CronogramaUiState,
    onBack: () -> Unit,
    onOpenTarea: (tareaId: String) -> Unit,
    onOpenTareas: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.cronograma_titulo)) },
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
        val cronograma = state.cronograma
        when {
            state.isLoading -> LoadingContent(Modifier.padding(padding))
            state.error != null -> Text(
                text = state.error.asMessage(),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .padding(padding)
                    .padding(24.dp)
            )
            cronograma == null -> Unit
            // Sin ninguna tarea cargada.
            cronograma.barras.isEmpty() && cronograma.sinFechas == 0 -> EmptyState(
                icon = Icons.Default.Timeline,
                title = stringResource(R.string.cronograma_vacio_titulo),
                description = stringResource(R.string.cronograma_vacio_descripcion),
                actionLabel = stringResource(R.string.obra_tareas_ver),
                onActionClick = onOpenTareas,
                modifier = Modifier.padding(padding)
            )
            // Hay tareas, pero ninguna con ambas fechas: no se inventan fechas para dibujarlas.
            cronograma.barras.isEmpty() -> Column(Modifier.padding(padding)) {
                SinFechasAviso(cantidad = cronograma.sinFechas, onOpenTareas = onOpenTareas)
                EmptyState(
                    icon = Icons.Default.Timeline,
                    title = stringResource(R.string.cronograma_sin_planificadas_titulo),
                    description = stringResource(R.string.cronograma_sin_planificadas_descripcion)
                )
            }
            // Un solo scroll vertical para todo el Gantt: títulos y barras se mueven juntos.
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            ) {
                if (cronograma.sinFechas > 0) {
                    SinFechasAviso(cantidad = cronograma.sinFechas, onOpenTareas = onOpenTareas)
                }
                GanttChart(cronograma = cronograma, onTareaClick = onOpenTarea)
            }
        }
    }
}

/** Informa cuántas tareas no se dibujan por no tener fechas y lleva al listado. */
@Composable
private fun SinFechasAviso(cantidad: Int, onOpenTareas: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = pluralStringResource(R.plurals.cronograma_sin_fechas, cantidad, cantidad),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onOpenTareas) {
            Text(stringResource(R.string.obra_tareas_ver))
        }
    }
    HorizontalDivider()
}
