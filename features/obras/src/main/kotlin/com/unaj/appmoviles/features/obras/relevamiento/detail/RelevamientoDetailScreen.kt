package com.unaj.appmoviles.features.obras.relevamiento.detail

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ImageNotSupported
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.unaj.appmoviles.features.obras.formatFecha
import com.unaj.appmoviles.obratrack.core.domain.model.Relevamiento
import com.unaj.appmoviles.obratrack.core.domain.ui.LoadingContent
import com.unaj.appmoviles.obratrack.core.ui.components.InfoRow
import com.unaj.appmoviles.obratrack.core.ui.components.MapPreviewCard

import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.unaj.appmoviles.features.obras.export.RelevamientoPdfExporter
import androidx.compose.ui.res.stringResource
import com.unaj.appmoviles.obras.R

@Composable
fun RelevamientoDetailRoute(
    obraId: String,
    relevamientoId: String,
    onBack: () -> Unit,
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
        onEditRelevamiento = onEditRelevamiento,
        snackbarHostState = snackbarHostState,
        onShareClick = {
            val relevamiento = state.relevamiento
            val obra = state.obra
            if (relevamiento != null) {
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
    onEditRelevamiento: (Relevamiento) -> Unit,
    snackbarHostState: SnackbarHostState,
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val relevamiento = state.relevamiento

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Detalle de Relevamiento") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    if (relevamiento != null) {
                        IconButton(onClick = onShareClick) {
                            Icon(Icons.Outlined.Share, contentDescription = stringResource(R.string.compartir))
                        }
                    }
                    if (state.isEditable && relevamiento != null) {
                        IconButton(onClick = { onEditRelevamiento(relevamiento) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Editar relevamiento")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (state.isEditable && relevamiento != null) {
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
                        text = state.error ?: "No pudimos encontrar este relevamiento",
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
