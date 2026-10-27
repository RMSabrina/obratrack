package com.unaj.appmoviles.features.obras.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.unaj.appmoviles.obras.R
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.unaj.appmoviles.features.obras.list.components.ObraCard
import com.unaj.appmoviles.obratrack.core.domain.model.Obra
import com.unaj.appmoviles.obratrack.core.domain.model.EstadoObra
import com.unaj.appmoviles.obratrack.core.domain.ui.LoadingContent
import com.unaj.appmoviles.obratrack.core.ui.components.SearchBarComponent
import com.unaj.appmoviles.features.obras.label

import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun ObrasPublicasListRoute(
    viewModel: ObrasPublicasListViewModel = hiltViewModel(),
    onOpen: (Obra) -> Unit,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ObrasPublicasListScreen(
        state = state,
        onOpen = onOpen,
        onBack = onBack,
        onToggleSubscription = viewModel::toggleSuscripcion
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ObrasPublicasListScreen(
    state: ObrasPublicasListUiState,
    onOpen: (Obra) -> Unit,
    onBack: () -> Unit,
    onToggleSubscription: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedEstado by rememberSaveable { mutableStateOf<EstadoObra?>(null) }
    var soloSuscripciones by rememberSaveable { mutableStateOf(false) }
    var showFilterMenu by remember { mutableStateOf(false) }
    val filteredObras = remember(searchQuery, selectedEstado, soloSuscripciones, state.obras, state.suscripciones) {
        filtrarObrasPublicas(state.obras, searchQuery, selectedEstado, state.suscripciones, soloSuscripciones)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = stringResource(R.string.obras_publicas_titulo), 
                        fontWeight = FontWeight.Bold 
                    ) 
                },
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
    ) { paddingValues ->
        if (state.isLoading) {
            LoadingContent(Modifier.padding(paddingValues))
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    SearchBarComponent(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        onFilterClick = { showFilterMenu = true },
                        placeholder = stringResource(R.string.obras_buscar)
                    )
                    Box(modifier = Modifier.align(Alignment.TopEnd).padding(top = 56.dp, end = 16.dp)) {
                        DropdownMenu(
                            expanded = showFilterMenu,
                            onDismissRequest = { showFilterMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = stringResource(R.string.obras_filtro_todas),
                                        fontWeight = if (selectedEstado == null && !soloSuscripciones) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    soloSuscripciones = false
                                    selectedEstado = null
                                    showFilterMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = stringResource(R.string.obras_filtro_seguidas),
                                        fontWeight = if (soloSuscripciones) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    soloSuscripciones = true
                                    showFilterMenu = false
                                }
                            )
                            HorizontalDivider()
                            EstadoObra.entries.forEach { estado ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = estado.label(),
                                            fontWeight = if (selectedEstado == estado) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        selectedEstado = estado
                                        showFilterMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
                if (state.errorSuscripcion) {
                    Text(
                        text = stringResource(R.string.obras_error_suscripcion),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
                when {
                    state.obras.isEmpty() -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.obras_publicas_vacia))
                        }
                    }
                    filteredObras.isEmpty() -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = stringResource(R.string.obras_busqueda_vacia),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(filteredObras, key = { it.id }) { obra ->
                                ObraCard(
                                    obra = obra,
                                    onClick = { onOpen(obra) },
                                    isFollowing = obra.id in state.suscripciones,
                                    followEnabled = obra.id !in state.suscripcionesPendientes,
                                    onFollowToggle = { onToggleSubscription(obra.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
