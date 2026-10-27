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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.unaj.appmoviles.features.obras.export.EstadoPdf
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
import com.unaj.appmoviles.features.obras.formatHora
import com.unaj.appmoviles.obras.R
import com.unaj.appmoviles.obratrack.core.domain.model.Relevamiento
import com.unaj.appmoviles.obratrack.core.domain.ui.LoadingContent
import com.unaj.appmoviles.obratrack.core.domain.ui.asDeleteMessage
import com.unaj.appmoviles.obratrack.core.ui.components.ErrorCarga
import com.unaj.appmoviles.obratrack.core.domain.error.RelevamientoError
import com.unaj.appmoviles.obratrack.core.ui.components.InfoRow
import com.unaj.appmoviles.obratrack.core.ui.components.MapPreviewCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
    val pdf by viewModel.pdf.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val errorMsg = stringResource(R.string.error_exportacion_pdf)
    val shareMsg = stringResource(R.string.compartir_relevamiento_via)

    // El PDF queda listo en el ViewModel: si se rotó la pantalla mientras se generaba, se comparte igual.
    LaunchedEffect(pdf) {
        when (val actual = pdf) {
            is EstadoPdf.Listo -> {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, actual.resultado)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, shareMsg))
                viewModel.pdfConsumido()
            }
            EstadoPdf.Error -> {
                snackbarHostState.showSnackbar(errorMsg)
                viewModel.pdfConsumido()
            }
            else -> Unit
        }
    }

    RelevamientoDetailScreen(
        state = state,
        onBack = onBack,
        onDeleteSuccess = onDeleteSuccess,
        onDeleteRelevamiento = viewModel::deleteRelevamiento,
        onDeleteErrorDismissed = viewModel::onDeleteErrorDismissed,
        onEditRelevamiento = onEditRelevamiento,
        snackbarHostState = snackbarHostState,
        onRetry = viewModel::reintentar,
        generandoPdf = pdf is EstadoPdf.Generando,
        onShareClick = {
            val relevamiento = state.relevamiento
            val obra = state.obra
            if (relevamiento != null && !state.isDeleting) {
                viewModel.generarPdf {
                    withContext(Dispatchers.IO) {
                        RelevamientoPdfExporter.exportRelevamientoToPdf(context, relevamiento, obra)
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
    generandoPdf: Boolean = false,
    onRetry: () -> Unit = {},
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

    val deleteErrorMessage = state.deleteError?.asDeleteMessage()
    LaunchedEffect(deleteErrorMessage) {
        if (deleteErrorMessage != null) {
            snackbarHostState.showSnackbar(deleteErrorMessage)
            onDeleteErrorDismissed()
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { if (!state.isDeleting) showDeleteDialog = false },
            title = { Text(stringResource(R.string.relevamiento_eliminar_titulo)) },
            text = { Text(stringResource(R.string.relevamiento_eliminar_confirmacion)) },
            confirmButton = {
                TextButton(
                    enabled = !state.isDeleting,
                    onClick = {
                        showDeleteDialog = false
                        onDeleteRelevamiento()
                    }
                ) {
                    Text(stringResource(R.string.eliminar), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !state.isDeleting,
                    onClick = { showDeleteDialog = false }
                ) {
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
                title = { Text(stringResource(R.string.relevamiento_detalle_titulo)) },
                navigationIcon = {
                    IconButton(
                        onClick = { if (!state.isDeleting) onBack() },
                        enabled = !state.isDeleting
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.volver))
                    }
                },
                actions = {
                    if (relevamiento != null) {
                        IconButton(
                            onClick = { if (!state.isDeleting && !generandoPdf) onShareClick() },
                            enabled = !state.isDeleting && !generandoPdf
                        ) {
                            if (generandoPdf) {
                                val generando = stringResource(R.string.pdf_generando)
                                CircularProgressIndicator(
                                    strokeWidth = 2.dp,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .semantics { contentDescription = generando }
                                )
                            } else {
                                Icon(Icons.Outlined.Share, contentDescription = stringResource(R.string.compartir))
                            }
                        }
                    }
                    if (state.isEditable && relevamiento != null) {
                        IconButton(
                            onClick = { if (!state.isDeleting) showDeleteDialog = true },
                            enabled = !state.isDeleting
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.relevamiento_eliminar),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                        IconButton(
                            onClick = { if (!state.isDeleting) onEditRelevamiento(relevamiento) },
                            enabled = !state.isDeleting
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.relevamiento_form_editar))
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
                    text = { Text(stringResource(R.string.relevamiento_form_editar)) }
                )
            }
        }
    ) { padding ->
        when {
            state.isLoading -> LoadingContent(Modifier.padding(padding))
            relevamiento == null -> {
                ErrorCarga(
                    error = state.error ?: RelevamientoError.NoEncontrado,
                    onRetry = onRetry,
                    modifier = Modifier.padding(padding)
                )
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
                                        text = stringResource(R.string.relevamiento_detalle_cliente, obra.cliente)
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
                                    contentDescription = stringResource(R.string.relevamiento_detalle_fotografia),
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
                                        text = stringResource(R.string.relevamiento_detalle_sin_foto),
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
                                text = stringResource(R.string.relevamiento_detalle_observaciones),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            InfoRow(
                                icon = Icons.Default.DateRange,
                                text = stringResource(R.string.relevamiento_detalle_fecha, formatFecha(relevamiento.fecha))
                            )
                            relevamiento.proximaVisita?.let { proxima ->
                                InfoRow(
                                    icon = Icons.Default.AddAlert,
                                    text = relevamiento.horaVisita?.let { hora ->
                                        stringResource(
                                            R.string.relevamiento_detalle_proxima_hora,
                                            formatFecha(proxima),
                                            formatHora(LocalContext.current, hora)
                                        )
                                    } ?: stringResource(R.string.relevamiento_detalle_proxima, formatFecha(proxima))
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = relevamiento.nota.ifBlank { stringResource(R.string.relevamiento_detalle_sin_notas) },
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Ubicación del Relevamiento
                    MapPreviewCard(
                        latitud = relevamiento.ubicacion?.latitud,
                        longitud = relevamiento.ubicacion?.longitud,
                        title = stringResource(R.string.relevamiento_detalle_ubicacion)
                    )

                    Spacer(modifier = Modifier.height(48.dp))
                }
            }
        }
    }
}
