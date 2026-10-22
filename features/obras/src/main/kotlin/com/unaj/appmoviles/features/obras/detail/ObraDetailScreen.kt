package com.unaj.appmoviles.features.obras.detail

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.unaj.appmoviles.features.obras.formatFecha
import com.unaj.appmoviles.features.obras.label
import com.unaj.appmoviles.obratrack.core.domain.model.EstadoObra
import com.unaj.appmoviles.obratrack.core.domain.model.Obra
import com.unaj.appmoviles.obratrack.core.domain.model.Relevamiento
import com.unaj.appmoviles.obratrack.core.domain.ui.LoadingContent
import com.unaj.appmoviles.obratrack.core.domain.ui.asMessage
import com.unaj.appmoviles.obratrack.core.ui.components.EmptyState
import com.unaj.appmoviles.obratrack.core.ui.components.InfoRow
import com.unaj.appmoviles.obratrack.core.ui.components.StatusBadge

@Composable
fun ObraDetailRoute(
    obraId: String,
    onBack: () -> Unit,
    onDeleteSuccess: () -> Unit,
    onEditObra: (Obra) -> Unit,
    onCreateRelevamiento: () -> Unit,
    onOpenRelevamiento: (Relevamiento) -> Unit,
    viewModel: ObraDetailViewModel = hiltViewModel(
        creationCallback = { factory: ObraDetailViewModel.Factory -> factory.create(obraId) }
    )
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ObraDetailScreen(
        state = state,
        onBack = onBack,
        onDeleteSuccess = onDeleteSuccess,
        onDeleteObra = viewModel::deleteObra,
        onDeleteErrorDismissed = viewModel::onDeleteErrorDismissed,
        onEditObra = onEditObra,
        onCreateRelevamiento = onCreateRelevamiento,
        onOpenRelevamiento = onOpenRelevamiento
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ObraDetailScreen(
    state: ObraDetailUiState,
    onBack: () -> Unit,
    onDeleteSuccess: () -> Unit,
    onDeleteObra: () -> Unit,
    onDeleteErrorDismissed: () -> Unit,
    onEditObra: (Obra) -> Unit,
    onCreateRelevamiento: () -> Unit,
    onOpenRelevamiento: (Relevamiento) -> Unit,
    modifier: Modifier = Modifier
) {
    val obra = state.obra
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    BackHandler(enabled = state.isDeleting) {}

    LaunchedEffect(state.isDeleted) {
        if (state.isDeleted) {
            onDeleteSuccess()
        }
    }

    val deleteErrorMessage = state.deleteError?.asMessage()
    LaunchedEffect(deleteErrorMessage) {
        if (deleteErrorMessage != null) {
            snackbarHostState.showSnackbar(deleteErrorMessage)
            onDeleteErrorDismissed()
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { if (!state.isDeleting) showDeleteDialog = false },
            title = { Text("Eliminar Obra") },
            text = { Text("¿Eliminar Obra y todos sus relevamientos, tareas y suscriptores? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(
                    enabled = !state.isDeleting,
                    onClick = {
                        showDeleteDialog = false
                        onDeleteObra()
                    }
                ) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !state.isDeleting,
                    onClick = { showDeleteDialog = false }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Detalle de obra") },
                navigationIcon = {
                    IconButton(
                        onClick = { if (!state.isDeleting) onBack() },
                        enabled = !state.isDeleting
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                actions = {
                    if (obra != null && state.isEditable) {
                        IconButton(
                            onClick = { if (!state.isDeleting) showDeleteDialog = true },
                            enabled = !state.isDeleting
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Eliminar obra",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                        IconButton(
                            onClick = { if (!state.isDeleting) onEditObra(obra) },
                            enabled = !state.isDeleting
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Editar obra"
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (obra != null && state.isEditable && !state.isDeleting) {
                FloatingActionButton(onClick = onCreateRelevamiento) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Agregar relevamiento"
                    )
                }
            }
        }
    ) { padding ->
        when {
            state.isLoading -> LoadingContent(Modifier.padding(padding))
            obra == null -> {
                Text(
                    text = state.error?.asMessage() ?: "No encontramos esa obra",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .padding(padding)
                        .padding(24.dp)
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        ObraHeaderCard(obra = obra)
                    }

                    item {
                        Text(
                            text = "Relevamientos",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }

                    if (state.relevamientos.isEmpty() && state.error == null) {
                        item {
                            EmptyState(
                                icon = Icons.Default.Info,
                                title = "Todavía no hay relevamientos",
                                description = "Esta obra no cuenta con relevamientos cargados.",
                                actionLabel = if (state.isEditable && !state.isDeleting) "Agregar el primero" else null,
                                onActionClick = if (state.isEditable && !state.isDeleting) onCreateRelevamiento else null
                            )
                        }
                    } else {
                        items(state.relevamientos, key = { it.id }) { relevamiento ->
                            RelevamientoCard(
                                relevamiento = relevamiento,
                                isEditable = state.isEditable && !state.isDeleting,
                                onClick = { if (!state.isDeleting) onOpenRelevamiento(relevamiento) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ObraHeaderCard(obra: Obra, modifier: Modifier = Modifier) {
    val statusColor = when (obra.estado) {
        EstadoObra.ACTIVA -> MaterialTheme.colorScheme.primaryContainer
        EstadoObra.PAUSADA -> MaterialTheme.colorScheme.surfaceVariant
        EstadoObra.FINALIZADA -> MaterialTheme.colorScheme.tertiaryContainer
    }
    val statusTextColor = when (obra.estado) {
        EstadoObra.ACTIVA -> MaterialTheme.colorScheme.onPrimaryContainer
        EstadoObra.PAUSADA -> MaterialTheme.colorScheme.onSurfaceVariant
        EstadoObra.FINALIZADA -> MaterialTheme.colorScheme.onTertiaryContainer
    }

    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = obra.nombre,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                StatusBadge(
                    text = obra.estado.label(),
                    backgroundColor = statusColor,
                    textColor = statusTextColor,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            InfoRow(
                icon = Icons.Default.Person,
                text = obra.cliente,
                contentDescription = "Cliente"
            )
            InfoRow(
                icon = Icons.Default.Place,
                text = obra.direccion,
                contentDescription = "Dirección"
            )

            val fechas = buildString {
                append("Inicio: ${formatFecha(obra.fechaInicio)}")
                val visita = obra.ultimaVisita
                if (visita != null) {
                    append(" · Última visita: ${formatFecha(visita)}")
                }
            }
            InfoRow(
                icon = Icons.Default.DateRange,
                text = fechas,
                contentDescription = "Fechas"
            )

            obra.proximaVisita?.let { proxima ->
                val isPast = proxima < System.currentTimeMillis()
                val proximaTexto = if (isPast) {
                    "Próxima visita: " + formatFecha(proxima) + " (Atrasada)"
                } else {
                    "Próxima visita: " + formatFecha(proxima)
                }
                InfoRow(
                    icon = if (isPast) Icons.Default.Warning else Icons.Default.Alarm,
                    text = proximaTexto,
                    contentDescription = "Próxima visita"
                )
            }
        }
    }
}

@Composable
private fun RelevamientoCard(
    relevamiento: Relevamiento,
    isEditable: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    val cardModifier = if (isEditable) {
        modifier.fillMaxWidth().clickable(onClick = onClick)
    } else {
        modifier.fillMaxWidth()
    }

    ElevatedCard(modifier = cardModifier) {
        Column {
            relevamiento.fotoUrl?.let { url ->
                AsyncImage(
                    model = url,
                    contentDescription = "Foto del relevamiento",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                )
            }
            
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = formatFecha(relevamiento.fecha),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (relevamiento.nota.isNotBlank()) {
                    Text(
                        text = relevamiento.nota,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                
                relevamiento.ubicacion?.let { ubicacion ->
                    Spacer(modifier = Modifier.height(4.dp))
                    TextButton(
                        onClick = {
                            val geo = Uri.parse(
                                "geo:${ubicacion.latitud},${ubicacion.longitud}?q=${ubicacion.latitud},${ubicacion.longitud}"
                            )
                            context.startActivity(Intent(Intent.ACTION_VIEW, geo))
                        },
                        contentPadding = PaddingValues(horizontal = 0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Text("Ver en el mapa")
                    }
                }
            }
        }
    }
}
