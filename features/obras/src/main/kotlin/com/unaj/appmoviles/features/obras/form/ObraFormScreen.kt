package com.unaj.appmoviles.features.obras.form

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationManager
import android.os.Build
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.filled.DateRange
import android.app.DatePickerDialog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.unaj.appmoviles.features.obras.estadoObraFromStorage
import com.unaj.appmoviles.features.obras.label
import com.unaj.appmoviles.features.obras.toStorageValue
import com.unaj.appmoviles.obratrack.core.domain.error.ObraError
import com.unaj.appmoviles.obratrack.core.domain.model.EstadoObra
import com.unaj.appmoviles.obratrack.core.domain.model.ObraDraft
import com.unaj.appmoviles.obratrack.core.domain.model.Ubicacion
import com.unaj.appmoviles.obratrack.core.domain.ui.asMessage

data class ObraFormInitial(
    val obraId: String?,
    val nombre: String,
    val cliente: String,
    val direccion: String,
    val estado: String,
    val publica: Boolean,
    val latitud: Double? = null,
    val longitud: Double? = null,
    val proximaVisita: Long? = null
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
    val context = LocalContext.current
    var nombre by rememberSaveable { mutableStateOf(initial.nombre) }
    var cliente by rememberSaveable { mutableStateOf(initial.cliente) }
    var direccion by rememberSaveable { mutableStateOf(initial.direccion) }
    var estado by rememberSaveable { mutableStateOf(initial.estado) }
    var publica by rememberSaveable { mutableStateOf(initial.publica) }
    var latitud by rememberSaveable { mutableStateOf(initial.latitud) }
    var longitud by rememberSaveable { mutableStateOf(initial.longitud) }
    var proximaVisita by rememberSaveable { mutableStateOf(initial.proximaVisita) }
    var ubicacionMensaje by rememberSaveable { mutableStateOf<String?>(null) }

    val pedirUbicacion = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { concedido ->
        if (concedido) {
            ubicacionMensaje = null
            capturarUbicacion(context) { ubicacion ->
                if (ubicacion != null) {
                    latitud = ubicacion.latitud
                    longitud = ubicacion.longitud
                } else {
                    ubicacionMensaje = "No pudimos obtener la señal de GPS actual."
                }
            }
        } else {
            ubicacionMensaje = "Se requiere permiso de ubicación para capturar GPS."
        }
    }

    val estadoActual = estadoObraFromStorage(estado)
    val error = (state as? ObraFormUiState.Error)?.error
    val isLoading = state is ObraFormUiState.Loading

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
                                estado = estadoActual,
                                ubicacion = if (latitud != null && longitud != null) Ubicacion(latitud!!, longitud!!) else null,
                                proximaVisita = proximaVisita
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
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Información General", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    OutlinedTextField(
                        value = nombre,
                        onValueChange = { nombre = it; onInputChanged() },
                        label = { Text("Nombre") },
                        leadingIcon = {
                            Icon(Icons.Default.Build, contentDescription = null)
                        },
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
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null)
                        },
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
                        leadingIcon = {
                            Icon(Icons.Default.Place, contentDescription = null)
                        },
                        isError = error == ObraError.DireccionVacia,
                        supportingText = {
                            if (error == ObraError.DireccionVacia) Text(error.asMessage())
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Sección: Ubicación Geográfica (RF4 y RF5)
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Ubicación Geográfica (GPS)", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    if (latitud != null && longitud != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Coordenadas asignadas", style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    "Lat: $latitud, Lng: $longitud",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = {
                                latitud = null
                                longitud = null
                            }) {
                                Icon(Icons.Default.Clear, contentDescription = "Borrar ubicación")
                            }
                        }

                        // Ajuste manual (RF4: editable manualmente)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = latitud?.toString() ?: "",
                                onValueChange = {
                                    latitud = it.toDoubleOrNull()
                                    onInputChanged()
                                },
                                label = { Text("Latitud") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = longitud?.toString() ?: "",
                                onValueChange = {
                                    longitud = it.toDoubleOrNull()
                                    onInputChanged()
                                },
                                label = { Text("Longitud") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.LocationOff, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Sin coordenadas asignadas",
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { pedirUbicacion.launch(Manifest.permission.ACCESS_FINE_LOCATION) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (latitud != null) "Actualizar coordenadas por GPS" else "Capturar ubicación por GPS")
                    }

                    if (ubicacionMensaje != null) {
                        Text(
                            text = ubicacionMensaje!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Estado", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        EstadoObra.entries.forEach { option ->
                            val (containerColor, labelColor) = when (option) {
                                EstadoObra.ACTIVA -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
                                EstadoObra.PAUSADA -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
                                EstadoObra.FINALIZADA -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
                            }

                            FilterChip(
                                selected = estadoActual == option,
                                onClick = { estado = option.toStorageValue(); onInputChanged() },
                                label = { Text(option.label()) },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = containerColor,
                                    selectedLabelColor = labelColor
                                )
                            )
                        }
                    }
                }
            }

            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Configuración", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Hacer la obra pública", style = MaterialTheme.typography.bodyLarge)
                        Switch(
                            checked = publica,
                            onCheckedChange = { publica = it; onInputChanged() }
                        )
                    }
                    Text(
                        text = "Una obra pública puede ser vista por otras personas, pero sólo tú puedes editarla o agregar relevamientos.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }
    }
}

@SuppressLint("MissingPermission")
private fun capturarUbicacion(context: Context, onResult: (Ubicacion?) -> Unit) {
    val manager = context.getSystemService(LocationManager::class.java)
    val provider = when {
        manager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
        manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
        else -> null
    }
    if (provider == null) {
        onResult(null)
        return
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        manager.getCurrentLocation(provider, null, context.mainExecutor) { location ->
            onResult(location?.let { Ubicacion(it.latitude, it.longitude) })
        }
    } else {
        @Suppress("DEPRECATION")
        manager.requestSingleUpdate(provider, { location ->
            onResult(location?.let { Ubicacion(it.latitude, it.longitude) })
        }, Looper.getMainLooper())
    }
}
