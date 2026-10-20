package com.unaj.appmoviles.features.obras.relevamiento.detail

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ImageNotSupported
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.unaj.appmoviles.features.obras.export.RelevamientoPdfExporter
import com.unaj.appmoviles.features.obras.formatFecha
import com.unaj.appmoviles.obras.R
import com.unaj.appmoviles.obratrack.core.domain.model.Relevamiento
import com.unaj.appmoviles.obratrack.core.domain.ui.LoadingContent
import com.unaj.appmoviles.obratrack.core.domain.ui.asMessage
import com.unaj.appmoviles.obratrack.core.ui.components.InfoRow
import com.unaj.appmoviles.obratrack.core.ui.components.MapPreviewCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun RelevamientoDetailRoute(
    obraId: String,
    relevamientoId: String,
    onBack: () -> Unit,
    onDeleteSuccess: () -> Unit,
    onEditRelevamiento: (Relevamiento) -> Unit
) {
    val viewModel = hiltViewModel<RelevamientoDetailViewModel, RelevamientoDetailViewModel.Factory>(
        creationCallback = { factory -> factory.create(obraId, relevamientoId) }
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val errorMsg = stringResource(R.string.error_exportacion_pdf)
    val shareMsg = stringResource(R.string.compartir_relevamiento_via)

    RelevamientoDetailScreen(
        state = state,
        onBack = onBack,
        onDeleteSuccess = onDeleteSuccess,
        onDeleteRelevamiento = viewModel::deleteRelevamiento,
        onDeleteErrorDismissed = viewModel::onDeleteErrorDismissed,
        onEditRelevamiento = onEditRelevamiento,
        snackbarHostState = snackbarHostState,
        onShareClick = {
            val relevamiento = state.relevamiento
            val obra = state.obra
            if (relevamiento != null && !state.isDeleting) {
                coroutineScope.launch(Dispatchers.IO) {
                    val uri = RelevamientoPdfExporter.exportRelevamientoToPdf(context, relevamiento, obra)
                    if (uri != null) {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/pdf"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        val chooser = Intent.createChooser(intent, shareMsg)
                        context.startActivity(chooser)
                    } else {
                        snackbarHostState.showSnackbar(errorMsg)
                    }
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RelevamientoDetailScreen(
    state: RelevamientoDetailUiState,
    onBack: () -> Unit,
    onDeleteSuccess: () -> Unit,
    onDeleteRelevamiento: () -> Unit,
    onDeleteErrorDismissed: () -> Unit,
    onEditRelevamiento: (Relevamiento) -> Unit,
    snackbarHostState: SnackbarHostState,
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val relevamiento = state.relevamiento
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }

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
            title = { Text("Eliminar Relevamiento") },
            text = { Text("¿Eliminar este relevamiento? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(
                    enabled = !state.isDeleting,
                    onClick = {
                        showDeleteDialog = false
                        onDeleteRelevamiento()
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
                title = { Text("Detalle de Relevamiento") },
                navigationIcon = {
                    IconButton(
                        onClick = { if (!state.isDeleting) onBack() },
                        enabled = !state.isDeleting
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    if (relevamiento != null) {
                        IconButton(
                            onClick = { if (!state.isDeleting) onShareClick() },
                            enabled = !state.isDeleting
                        ) {
                            Icon(Icons.Outlined.Share, contentDescription = stringResource(R.string.compartir))
                        }
                    }
                    if (state.isEditable && relevamiento != null) {
                        IconButton(
                            onClick = { if (!state.isDeleting) showDeleteDialog = true },
                            enabled = !state.isDeleting
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Eliminar relevamiento",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                        IconButton(
                            onClick = { if (!state.isDeleting) onEditRelevamiento(relevamiento) },
                            enabled = !state.isDeleting
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Editar relevamiento")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (state.isEditable && relevamiento != null && !state.isDeleting) {
                ExtendedFloatingActionButton(
                    onClick = { onEditRelevamiento(relevamiento) },
                    icon = { Icon(Icons.Default.Edit, contentDescription = null) },
                    text = { Text("Editar relevamiento") }
                )
            }
        }
    ) { padding ->
        when {
            state.isLoading -> LoadingContent(Modifier.padding(padding))
            relevamiento == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.error?.asMessage() ?: "No pudimos encontrar este relevamiento",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Tarjeta Obra vinculada
                    state.obra?.let { obra ->
                        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Apartment,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = obra.nombre,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                if (obra.cliente.isNotBlank()) {
                                    InfoRow(
                                        icon = Icons.Default.Person,
                                        text = "Cliente: " + obra.cliente
                                    )
                                }
                                if (obra.direccion.isNotBlank()) {
                                    InfoRow(
                                        icon = Icons.Default.Place,
                                        text = obra.direccion
                                    )
                                }
                            }
                        }
                    }

                    // Fotografía Grande
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            if (!relevamiento.fotoUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = relevamiento.fotoUrl,
                                    contentDescription = "Fotografía del relevamiento",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(280.dp)
                                )
                            } else {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ImageNotSupported,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Sin fotografía adjunta",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Observaciones / Notas
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Observaciones de la visita",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            InfoRow(
                                icon = Icons.Default.DateRange,
                                text = "Fecha de visita: " + formatFecha(relevamiento.fecha)
                            )
                            relevamiento.proximaVisita?.let { proxima ->
                                InfoRow(
                                    icon = Icons.Default.AddAlert,
                                    text = "Próxima visita planificada: " + formatFecha(proxima)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (relevamiento.nota.isBlank()) "Sin notas registradas." else relevamiento.nota,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Ubicación del Relevamiento
                    MapPreviewCard(
                        latitud = relevamiento.ubicacion?.latitud,
                        longitud = relevamiento.ubicacion?.longitud,
                        title = "Ubicación del Relevamiento"
                    )

                    Spacer(modifier = Modifier.height(48.dp))
                }
            }
        }
    }
}
