package com.unaj.appmoviles.features.obras.tareas.detail

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.unaj.appmoviles.features.obras.formatFecha
import com.unaj.appmoviles.features.obras.tareas.labelRes
import com.unaj.appmoviles.obras.R
import com.unaj.appmoviles.obratrack.core.domain.model.EstadoTarea
import com.unaj.appmoviles.obratrack.core.domain.model.Tarea
import com.unaj.appmoviles.obratrack.core.domain.ui.LoadingContent
import com.unaj.appmoviles.obratrack.core.domain.ui.asMessage
import com.unaj.appmoviles.obratrack.core.ui.components.InfoRow
import com.unaj.appmoviles.obratrack.core.ui.components.StatusBadge

@Composable
fun TareaDetailRoute(
    obraId: String,
    tareaId: String,
    onBack: () -> Unit,
    onDeleteSuccess: () -> Unit,
    onEditTarea: () -> Unit,
    viewModel: TareaDetailViewModel = hiltViewModel(
        creationCallback = { factory: TareaDetailViewModel.Factory -> factory.create(obraId, tareaId) }
    )
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TareaDetailScreen(
        state = state,
        onBack = onBack,
        onDeleteSuccess = onDeleteSuccess,
        onEditTarea = onEditTarea,
        onCompletar = viewModel::completar,
        onEliminar = viewModel::eliminar,
        onActionErrorShown = viewModel::onActionErrorShown,
        onDeleteErrorShown = viewModel::onDeleteErrorShown
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TareaDetailScreen(
    state: TareaDetailUiState,
    onBack: () -> Unit,
    onDeleteSuccess: () -> Unit,
    onEditTarea: () -> Unit,
    onCompletar: () -> Unit,
    onEliminar: () -> Unit,
    onActionErrorShown: () -> Unit,
    onDeleteErrorShown: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tarea = state.tarea
    val isBusy = state.isDeleting || state.isCompleting
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    BackHandler(enabled = state.isDeleting) {}

    LaunchedEffect(state.isDeleted) {
        if (state.isDeleted) onDeleteSuccess()
    }

    val deleteErrorMessage = state.deleteError?.asMessage()
    LaunchedEffect(deleteErrorMessage) {
        if (deleteErrorMessage != null) {
            snackbarHostState.showSnackbar(deleteErrorMessage)
            onDeleteErrorShown()
        }
    }

    val actionErrorMessage = state.actionError?.asMessage()
    LaunchedEffect(actionErrorMessage) {
        if (actionErrorMessage != null) {
            snackbarHostState.showSnackbar(actionErrorMessage)
            onActionErrorShown()
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.tarea_eliminar)) },
            text = { Text(stringResource(R.string.tarea_eliminar_confirmacion)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onEliminar()
                }) {
                    Text(stringResource(R.string.eliminar), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancelar))
                }
            }
        )
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tarea_detalle_titulo)) },
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !state.isDeleting) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.volver))
                    }
                },
                actions = {
                    if (tarea != null) {
                        IconButton(onClick = { showDeleteDialog = true }, enabled = !isBusy) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.tarea_eliminar),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                        IconButton(onClick = onEditTarea, enabled = !isBusy) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = stringResource(R.string.tarea_editar)
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        when {
            state.isLoading -> LoadingContent(Modifier.padding(padding))
            tarea == null -> Text(
                text = state.error?.asMessage().orEmpty(),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .padding(padding)
                    .padding(24.dp)
            )
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TareaDetailCard(tarea = tarea, atrasada = state.atrasada)

                // Reabrir una tarea completada se hace editándola.
                if (tarea.estado != EstadoTarea.COMPLETADA) {
                    Button(
                        onClick = onCompletar,
                        enabled = !isBusy,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (state.isCompleting) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.tarea_guardando))
                        } else {
                            Icon(Icons.Default.CheckCircle, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.tarea_marcar_completada))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TareaDetailCard(tarea: Tarea, atrasada: Boolean, modifier: Modifier = Modifier) {
    val (badgeBackground, badgeText) = when (tarea.estado) {
        EstadoTarea.PENDIENTE -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        EstadoTarea.EN_CURSO -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        EstadoTarea.COMPLETADA -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
    }
    val inicio = tarea.fechaInicio
    val fin = tarea.fechaFin

    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = tarea.titulo,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusBadge(
                    text = stringResource(tarea.estado.labelRes()),
                    backgroundColor = badgeBackground,
                    textColor = badgeText
                )
                if (atrasada) {
                    StatusBadge(
                        text = stringResource(R.string.tarea_atrasada),
                        backgroundColor = MaterialTheme.colorScheme.errorContainer,
                        textColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            InfoRow(
                icon = Icons.Default.Category,
                text = stringResource(tarea.categoria.labelRes()),
                contentDescription = stringResource(R.string.tarea_campo_categoria)
            )
            InfoRow(
                icon = if (atrasada) Icons.Default.Warning else Icons.Default.DateRange,
                text = if (inicio != null && fin != null) {
                    stringResource(R.string.tareas_rango_fechas, formatFecha(inicio), formatFecha(fin))
                } else {
                    stringResource(R.string.tareas_sin_fechas)
                },
                contentDescription = stringResource(R.string.tarea_campo_fechas)
            )
            Text(
                text = stringResource(R.string.tarea_descripcion),
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = tarea.descripcion.ifBlank { stringResource(R.string.tarea_sin_descripcion) },
                style = MaterialTheme.typography.bodyMedium,
                color = if (tarea.descripcion.isBlank()) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}
