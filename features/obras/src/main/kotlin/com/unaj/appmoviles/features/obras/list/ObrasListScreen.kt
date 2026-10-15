package com.unaj.appmoviles.features.obras.list

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.unaj.appmoviles.features.obras.list.components.ObraCard
import com.unaj.appmoviles.obratrack.core.domain.model.Obra
import com.unaj.appmoviles.obratrack.core.domain.ui.LoadingContent
import com.unaj.appmoviles.obratrack.core.domain.ui.asMessage
import com.unaj.appmoviles.obratrack.core.ui.components.SearchBarComponent

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import com.unaj.appmoviles.obratrack.core.domain.model.EstadoObra
import com.unaj.appmoviles.features.obras.label
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment

@Composable
fun ObrasListRoute(
    viewModel: ObrasListViewModel,
    onCreate: () -> Unit,
    onOpen: (Obra) -> Unit,
    onLogout: () -> Unit,
    onBack: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ObrasListScreen(
        state = state,
        onCreate = onCreate,
        onOpen = onOpen,
        onLogout = onLogout,
        onBack = onBack
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
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedEstado by remember { mutableStateOf<EstadoObra?>(null) }
    var showFilterMenu by remember { mutableStateOf(false) }
    
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
        containerColor = Color(0xFFFBFDFB),
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Mis obras", 
                        fontWeight = FontWeight.Bold 
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = "Volver"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Cerrar sesión"
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
                contentColor = Color.White
            ) { 
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Nueva obra"
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
                    placeholder = "Buscar obra..."
                )
                
                Box(modifier = Modifier.align(Alignment.TopEnd).padding(top = 48.dp, end = 16.dp)) {
                    DropdownMenu(
                        expanded = showFilterMenu,
                        onDismissRequest = { showFilterMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    text = "Todas",
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
                    Text(
                        text = state.error.asMessage(),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(24.dp)
                    )
                }
                state.obras.isEmpty() -> {
                    Text(
                        text = "Todavía no cargaste obras.",
                        modifier = Modifier.padding(24.dp)
                    )
                }
                filteredObras.isEmpty() -> {
                    Text(
                        text = "No se encontraron obras para la búsqueda.",
                        modifier = Modifier.padding(24.dp),
                        color = Color.Gray
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
