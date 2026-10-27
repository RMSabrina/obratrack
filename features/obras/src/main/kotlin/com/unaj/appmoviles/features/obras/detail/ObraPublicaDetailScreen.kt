package com.unaj.appmoviles.features.obras.detail

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.unaj.appmoviles.features.obras.formatFecha
import com.unaj.appmoviles.features.obras.label
import com.unaj.appmoviles.obras.R
import com.unaj.appmoviles.obratrack.core.domain.model.Relevamiento
import com.unaj.appmoviles.obratrack.core.domain.ui.LoadingContent

import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun ObraPublicaDetailRoute(
    obraId: String,
    onBack: () -> Unit,
    onOpenRelevamiento: (Relevamiento) -> Unit = {},
    viewModel: ObraPublicaDetailViewModel = hiltViewModel(
        creationCallback = { factory: ObraPublicaDetailViewModel.Factory -> factory.create(obraId) }
    )
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ObraPublicaDetailScreen(
        state = state,
        onBack = onBack,
        onOpenRelevamiento = onOpenRelevamiento
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ObraPublicaDetailScreen(
    state: ObraPublicaDetailUiState,
    onBack: () -> Unit,
    onOpenRelevamiento: (Relevamiento) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val obra = state.obra
    var pestana by rememberSaveable { mutableStateOf(ObraDetailTab.RESUMEN) }
    val scrollResumen = rememberLazyListState()
    val scrollRelevamientos = rememberLazyListState()
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(obra?.nombre ?: stringResource(R.string.obra_publica_detalle_titulo)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.volver))
                    }
                }
            )
        }
    ) { padding ->
        when {
            state.isLoading -> LoadingContent(Modifier.padding(padding))
            obra == null -> {
                Text(
                    text = stringResource(if (state.error) R.string.obra_publica_error else R.string.obra_publica_no_encontrada),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .padding(padding)
                        .padding(24.dp)
                )
            }
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Un no dueño nunca ve Tareas ni Cronograma: son privados aunque la obra sea pública.
                val pestanas = pestanasVisibles(esDueno = false)
                PrimaryTabRow(selectedTabIndex = pestanas.indexOf(pestana)) {
                    pestanas.forEach { tab ->
                        Tab(
                            selected = tab == pestana,
                            onClick = { pestana = tab },
                            text = { Text(stringResource(tab.labelRes()), maxLines = 1) }
                        )
                    }
                }
                // En el Resumen, solo los más recientes; el historial completo, en su pestaña.
                val relevamientos = if (pestana == ObraDetailTab.RESUMEN) {
                    state.relevamientos.recientes()
                } else {
                    state.relevamientos
                }
                // Un estado de scroll por pestaña: si no, volver a Resumen conserva el scroll de
                // Relevamientos y la obra queda fuera de la vista.
                LazyColumn(
                    state = if (pestana == ObraDetailTab.RESUMEN) scrollResumen else scrollRelevamientos,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (pestana == ObraDetailTab.RESUMEN) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(obra.cliente, style = MaterialTheme.typography.titleMedium)
                                Text(obra.direccion)
                                Text(
                                    stringResource(R.string.obra_publica_estado, obra.estado.label()),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        item { HorizontalDivider() }
                        item {
                            Text(
                                stringResource(R.string.obra_seccion_relevamientos),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                    if (state.relevamientosError) {
                        item { RelevamientosErrorMensaje() }
                    } else if (relevamientos.isEmpty()) {
                        item { Text(stringResource(R.string.obra_relevamientos_vacio)) }
                    } else {
                        items(relevamientos, key = { it.id }) { relevamiento ->
                            RelevamientoPublicoCard(relevamiento = relevamiento, onClick = { onOpenRelevamiento(relevamiento) })
                        }
                    }
                    if (pestana == ObraDetailTab.RESUMEN && state.relevamientos.size > relevamientos.size) {
                        item {
                            TextButton(
                                onClick = { pestana = ObraDetailTab.RELEVAMIENTOS },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(stringResource(R.string.obra_relevamientos_ver_todos, state.relevamientos.size))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RelevamientoPublicoCard(relevamiento: Relevamiento, onClick: () -> Unit = {}) {
    val context = LocalContext.current
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(formatFecha(relevamiento.fecha), style = MaterialTheme.typography.labelLarge)
            relevamiento.fotoUrl?.let { url ->
                AsyncImage(
                    model = url,
                    contentDescription = stringResource(R.string.relevamiento_form_foto),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                )
            }
            Text(relevamiento.nota, maxLines = 5)
            relevamiento.ubicacion?.let { ubicacion ->
                TextButton(onClick = {
                    val geo = Uri.parse(
                        "geo:${ubicacion.latitud},${ubicacion.longitud}?q=${ubicacion.latitud},${ubicacion.longitud}"
                    )
                    context.startActivity(Intent(Intent.ACTION_VIEW, geo))
                }) {
                    Text(stringResource(R.string.ver_en_mapa))
                }
            }
        }
    }
}
