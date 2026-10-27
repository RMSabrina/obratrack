package com.unaj.appmoviles.features.obras.list

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.unaj.appmoviles.obras.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.unaj.appmoviles.features.obras.list.components.ObraCard
import com.unaj.appmoviles.obratrack.core.domain.model.Obra
import com.unaj.appmoviles.obratrack.core.domain.ui.LoadingContent
import com.unaj.appmoviles.obratrack.core.ui.components.ErrorCarga
import com.unaj.appmoviles.obratrack.core.ui.components.SearchBarComponent

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import com.unaj.appmoviles.obratrack.core.domain.model.EstadoObra
import com.unaj.appmoviles.features.obras.label
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment

import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun ObrasListRoute(
    viewModel: ObrasListViewModel = hiltViewModel(),
    onCreate: () -> Unit,
    onOpen: (Obra) -> Unit,
    onLogout: () -> Unit,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ObrasListScreen(
        state = state,
        onCreate = onCreate,
        onOpen = onOpen,
        onLogout = onLogout,
        onBack = onBack,
        onRetry = viewModel::reintentar
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ObrasListScreen(
    state: ObrasListUiState,
    onCreate: () -> Unit,
    onOpen: (Obra) -> Unit,
    onLogout: () -> Unit,
    onBack: () -> Unit,
    onRetry: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    // Se guardan al rotar la pantalla; el estado se guarda por nombre.
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedEstado by rememberSaveable(
        stateSaver = Saver<EstadoObra?, String>(
            save = { it?.name.orEmpty() },
            restore = { nombre -> EstadoObra.entries.firstOrNull { it.name == nombre } }
        )
    ) { mutableStateOf<EstadoObra?>(null) }
    var showFilterMenu by rememberSaveable { mutableStateOf(false) }
    var showLogoutDialog by rememberSaveable { mutableStateOf(false) }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text(stringResource(R.string.obras_cerrar_sesion_titulo)) },
            text = { Text(stringResource(R.string.obras_cerrar_sesion_mensaje)) },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    onLogout()
                }) { Text(stringResource(R.string.obras_cerrar_sesion)) }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text(stringResource(R.string.cancelar)) }
            }
        )
    }
    
    // Simple local filter for the list based on the search query and selected state
    val filteredObras = remember(searchQuery, selectedEstado, state.obras) {
        state.obras.filter { obra ->
            val matchesSearch = if (searchQuery.isBlank()) true else obra.nombre.contains(searchQuery, ignoreCase = true)
            val matchesEstado = if (selectedEstado == null) true else obra.estado == selectedEstado
            matchesSearch && matchesEstado
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = stringResource(R.string.obras_lista_titulo), 
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
                },
                actions = {
                    IconButton(onClick = { showLogoutDialog = true }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = stringResource(R.string.obras_cerrar_sesion)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreate,
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) { 
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.obras_nueva_obra)
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                SearchBarComponent(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    onFilterClick = { showFilterMenu = true },
                    modifier = Modifier,
                    placeholder = stringResource(R.string.obras_buscar)
                )
                
                Box(modifier = Modifier.align(Alignment.TopEnd).padding(top = 48.dp, end = 16.dp)) {
                    DropdownMenu(
                        expanded = showFilterMenu,
                        onDismissRequest = { showFilterMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    text = stringResource(R.string.obras_filtro_todas),
                                    fontWeight = if (selectedEstado == null) FontWeight.Bold else FontWeight.Normal
                                ) 
                            },
                            onClick = { 
                                selectedEstado = null
                                showFilterMenu = false
                            }
                        )
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

            when {
                state.isLoading -> {
                    LoadingContent(Modifier.weight(1f))
                }
                state.error != null && state.obras.isEmpty() -> {
                    ErrorCarga(
                        error = state.error,
                        onRetry = onRetry,
                        modifier = Modifier.weight(1f)
                    )
                }
                state.obras.isEmpty() -> {
                    Text(
                        text = stringResource(R.string.obras_lista_vacia),
                        modifier = Modifier.padding(24.dp)
                    )
                }
                filteredObras.isEmpty() -> {
                    Text(
                        text = stringResource(R.string.obras_busqueda_vacia),
                        modifier = Modifier.padding(24.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(filteredObras, key = { it.id }) { obra ->
                            ObraCard(obra = obra, onClick = { onOpen(obra) })
                        }
                    }
                }
            }
        }
    }
}
