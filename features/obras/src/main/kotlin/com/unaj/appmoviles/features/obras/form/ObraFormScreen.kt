package com.unaj.appmoviles.features.obras.form

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.tasks.CancellationTokenSource
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.material.icons.filled.Map
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.unaj.appmoviles.obras.R
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
import com.unaj.appmoviles.obratrack.core.ui.components.MapPickerDialog
import com.unaj.appmoviles.obratrack.core.ui.components.MapPreviewCard
import androidx.compose.material3.CircularProgressIndicator
import com.unaj.appmoviles.obratrack.core.domain.ui.asMessage
import androidx.hilt.navigation.compose.hiltViewModel

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
    initial: ObraFormInitial,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: ObraFormViewModel = hiltViewModel(
        creationCallback = { factory: ObraFormViewModel.Factory -> factory.create(initial.obraId) }
    )
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
    var showMapPicker by rememberSaveable { mutableStateOf(false) }
    var proximaVisita by rememberSaveable { mutableStateOf(initial.proximaVisita) }
    var ubicacionMensaje by rememberSaveable { mutableStateOf<String?>(null) }
    var isCapturingLocation by remember { mutableStateOf(false) }
    var cancellationTokenSource by remember { mutableStateOf<CancellationTokenSource?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            cancellationTokenSource?.cancel()
        }
    }

    val pedirUbicacion = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { concedido ->
        if (concedido) {
            ubicacionMensaje = null
            isCapturingLocation = true
            cancellationTokenSource = capturarUbicacion(context) { ubicacion ->
                isCapturingLocation = false
                cancellationTokenSource = null
                if (ubicacion != null) {
                    latitud = ubicacion.latitud
                    longitud = ubicacion.longitud
                } else {
                    ubicacionMensaje = context.getString(R.string.obras_error_ubicacion_no_disponible)
                }
            }
        } else {
            isCapturingLocation = false
            ubicacionMensaje = context.getString(R.string.obras_error_permiso_ubicacion)
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
                        MapPreviewCard(
                            latitud = latitud,
                            longitud = longitud,
                            modifier = Modifier.fillMaxWidth(),
                            bottomActions = {
                                OutlinedButton(
                                    onClick = { 
                                        latitud = null
                                        longitud = null
                                    },
                                    enabled = !isCapturingLocation,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(context.getString(R.string.obras_borrar_ubicacion))
                                }
                            }
                        )
                    } else {
                        MapPreviewCard(
                            latitud = null,
                            longitud = null,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = { pedirUbicacion.launch(Manifest.permission.ACCESS_FINE_LOCATION) },
                            enabled = !isCapturingLocation,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (isCapturingLocation) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text(context.getString(R.string.obras_capturando_ubicacion))
                            } else {
                                Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(if (latitud != null) context.getString(R.string.obras_actualizar_ubicacion) else context.getString(R.string.obras_capturar_ubicacion))
                            }
                        }

                        OutlinedButton(
                            onClick = { showMapPicker = true },
                            enabled = !isCapturingLocation,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(context.getString(R.string.obras_ajustar_en_mapa))
                        }
                    }

                    if (showMapPicker) {
                        MapPickerDialog(
                            initialLatitud = latitud,
                            initialLongitud = longitud,
                            onDismissRequest = { showMapPicker = false },
                            onLocationSelected = { selectedLat, selectedLng ->
                                latitud = selectedLat
                                longitud = selectedLng
                                showMapPicker = false
                                onInputChanged()
                            }
                        )
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
private fun capturarUbicacion(context: Context, onResult: (Ubicacion?) -> Unit): CancellationTokenSource {
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    val cancellationTokenSource = CancellationTokenSource()

    fusedLocationClient.lastLocation.addOnSuccessListener { lastLocation ->
        if (lastLocation != null && (System.currentTimeMillis() - lastLocation.time) < 120_000) {
            onResult(Ubicacion(lastLocation.latitude, lastLocation.longitude))
            return@addOnSuccessListener
        }

        val request = CurrentLocationRequest.Builder()
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .setMaxUpdateAgeMillis(60_000)
            .setDurationMillis(15_000)
            .build()

        fusedLocationClient.getCurrentLocation(request, cancellationTokenSource.token)
            .addOnSuccessListener { location ->
                onResult(location?.let { Ubicacion(it.latitude, it.longitude) })
            }
            .addOnFailureListener {
                onResult(null)
            }
            .addOnCanceledListener {
                onResult(null)
            }
    }.addOnFailureListener {
        onResult(null)
    }

    return cancellationTokenSource
}
