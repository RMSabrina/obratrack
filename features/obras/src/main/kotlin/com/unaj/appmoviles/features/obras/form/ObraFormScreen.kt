package com.unaj.appmoviles.features.obras.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.unaj.appmoviles.features.obras.estadoObraFromStorage
import com.unaj.appmoviles.features.obras.label
import com.unaj.appmoviles.features.obras.toStorageValue
import com.unaj.appmoviles.obratrack.core.domain.error.ObraError
import com.unaj.appmoviles.obratrack.core.domain.model.EstadoObra
import com.unaj.appmoviles.obratrack.core.domain.model.ObraDraft
import com.unaj.appmoviles.obratrack.core.domain.ui.asMessage

data class ObraFormInitial(
    val obraId: String?,
    val nombre: String,
    val cliente: String,
    val direccion: String,
    val estado: String,
    val publica: Boolean
)

@Composable
fun ObraFormRoute(
    viewModel: ObraFormViewModel,
    initial: ObraFormInitial,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state) {
        if (state is ObraFormUiState.Saved) onSaved()
    }
    ObraFormScreen(
        initial = initial,
        state = state,
        onBack = onBack,
        onSave = viewModel::save,
        onInputChanged = viewModel::onErrorShown
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ObraFormScreen(
    initial: ObraFormInitial,
    state: ObraFormUiState,
    onBack: () -> Unit,
    onSave: (ObraDraft) -> Unit,
    onInputChanged: () -> Unit,
    modifier: Modifier = Modifier
) {
    var nombre by rememberSaveable { mutableStateOf(initial.nombre) }
    var cliente by rememberSaveable { mutableStateOf(initial.cliente) }
    var direccion by rememberSaveable { mutableStateOf(initial.direccion) }
    var estado by rememberSaveable { mutableStateOf(initial.estado) }
    var publica by rememberSaveable { mutableStateOf(initial.publica) }
    val error = (state as? ObraFormUiState.Error)?.error
    val isLoading = state is ObraFormUiState.Loading
    val estadoActual = estadoObraFromStorage(estado)

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(if (initial.obraId == null) "Nueva obra" else "Editar obra") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentPadding = PaddingValues(16.dp)
            ) {
                Button(
                    onClick = {
                        onSave(
                            ObraDraft(
                                nombre = nombre,
                                cliente = cliente,
                                direccion = direccion,
                                publica = publica,
                                estado = estadoActual
                            )
                        )
                    },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Guardando...")
                    } else {
                        Text("Guardar Obra")
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Información General", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it; onInputChanged() },
                    label = { Text("Nombre") },
                    isError = error == ObraError.NombreVacio,
                    supportingText = {
                        if (error == ObraError.NombreVacio) Text(error.asMessage())
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = cliente,
                    onValueChange = { cliente = it; onInputChanged() },
                    label = { Text("Cliente") },
                    isError = error == ObraError.ClienteVacio,
                    supportingText = {
                        if (error == ObraError.ClienteVacio) Text(error.asMessage())
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = direccion,
                    onValueChange = { direccion = it; onInputChanged() },
                    label = { Text("Dirección") },
                    isError = error == ObraError.DireccionVacia,
                    supportingText = {
                        if (error == ObraError.DireccionVacia) Text(error.asMessage())
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Estado", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EstadoObra.entries.forEach { option ->
                        FilterChip(
                            selected = estadoActual == option,
                            onClick = { estado = option.toStorageValue(); onInputChanged() },
                            label = { Text(option.label()) }
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Configuración", style = MaterialTheme.typography.titleMedium)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Obra pública", style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = publica,
                        onCheckedChange = { publica = it; onInputChanged() }
                    )
                }
            }

            if (
                error != null &&
                error != ObraError.NombreVacio &&
                error != ObraError.ClienteVacio &&
                error != ObraError.DireccionVacia
            ) {
                Text(
                    text = error.asMessage(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
