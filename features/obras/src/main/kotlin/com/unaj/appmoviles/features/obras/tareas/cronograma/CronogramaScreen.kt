package com.unaj.appmoviles.features.obras.tareas.cronograma

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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

/** Contenido de la pestaña Cronograma del detalle de obra. */
@Composable
fun CronogramaRoute(
    obraId: String,
    onOpenTarea: (tareaId: String) -> Unit,
    onOpenTareas: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CronogramaViewModel = hiltViewModel(
        creationCallback = { factory: CronogramaViewModel.Factory -> factory.create(obraId) }
    )
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    CronogramaContent(
        state = state,
        onOpenTarea = onOpenTarea,
        onOpenTareas = onOpenTareas,
        modifier = modifier
    )
}

@Composable
fun CronogramaContent(
    state: CronogramaUiState,
    onOpenTarea: (tareaId: String) -> Unit,
    onOpenTareas: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cronograma = state.cronograma
    when {
        state.isLoading -> LoadingContent(modifier)
        state.error != null -> Text(
            text = state.error.asMessage(),
            color = MaterialTheme.colorScheme.error,
            modifier = modifier.padding(24.dp)
        )
        cronograma == null -> Unit
        // Sin ninguna tarea cargada.
        cronograma.barras.isEmpty() && cronograma.sinFechas == 0 -> EmptyState(
            icon = Icons.Default.Timeline,
            title = stringResource(R.string.cronograma_vacio_titulo),
            description = stringResource(R.string.cronograma_vacio_descripcion),
            actionLabel = stringResource(R.string.cronograma_ir_a_tareas),
            onActionClick = onOpenTareas,
            modifier = modifier
        )
        // Hay tareas, pero ninguna con ambas fechas: no se inventan fechas para dibujarlas.
        cronograma.barras.isEmpty() -> Column(modifier) {
            SinFechasAviso(cantidad = cronograma.sinFechas, onOpenTareas = onOpenTareas)
            EmptyState(
                icon = Icons.Default.Timeline,
                title = stringResource(R.string.cronograma_sin_planificadas_titulo),
                description = stringResource(R.string.cronograma_sin_planificadas_descripcion)
            )
        }
        // Un solo scroll vertical para todo el Gantt: títulos y barras se mueven juntos.
        else -> Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            if (cronograma.sinFechas > 0) {
                SinFechasAviso(cantidad = cronograma.sinFechas, onOpenTareas = onOpenTareas)
            }
            GanttChart(cronograma = cronograma, onTareaClick = onOpenTarea)
        }
    }
}

/** Informa cuántas tareas no se dibujan por no tener fechas y lleva a la pestaña Tareas. */
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
            Text(stringResource(R.string.cronograma_ir_a_tareas))
        }
    }
    HorizontalDivider()
}
