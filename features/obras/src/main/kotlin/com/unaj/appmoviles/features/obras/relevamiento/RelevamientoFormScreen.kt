package com.unaj.appmoviles.features.obras.relevamiento

import android.annotation.SuppressLint
import android.Manifest
import android.content.Context
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.unaj.appmoviles.features.obras.formatFecha
import com.unaj.appmoviles.obratrack.core.domain.error.RelevamientoError
import com.unaj.appmoviles.obratrack.core.domain.model.RelevamientoDraft
import com.unaj.appmoviles.obratrack.core.domain.model.Ubicacion
import com.unaj.appmoviles.obratrack.core.domain.ui.asMessage
import java.io.File
import java.util.Calendar
import java.util.TimeZone
import androidx.hilt.navigation.compose.hiltViewModel

data class RelevamientoFormInitial(
    val obraId: String,
    val relevamientoId: String?,
    val nota: String,
    val fechaMillis: Long,
    val fotoUrl: String? = null,
    val latitud: Double? = null,
    val longitud: Double? = null
)

@Composable
fun RelevamientoFormRoute(
    initial: RelevamientoFormInitial,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: RelevamientoFormViewModel = hiltViewModel(
        creationCallback = { factory: RelevamientoFormViewModel.Factory -> 
            factory.create(initial.obraId, initial.relevamientoId) 
        }
    )
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state) {
        if (state is RelevamientoFormUiState.Saved) onSaved()
    }
    RelevamientoFormScreen(
        initial = initial,
        state = state,
        onBack = onBack,
        onSave = viewModel::save,
        onInputChanged = viewModel::onErrorShown
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RelevamientoFormScreen(
    initial: RelevamientoFormInitial,
    state: RelevamientoFormUiState,
    onBack: () -> Unit,
    onSave: (RelevamientoDraft, ByteArray?) -> Unit,
    onInputChanged: () -> Unit,
    modifier: Modifier = Modifier
) {
    var nota by rememberSaveable { mutableStateOf(initial.nota) }
    var fecha by rememberSaveable {
        mutableLongStateOf(if (initial.fechaMillis > 0L) initial.fechaMillis else startOfTodayUtc())
    }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var fotoLocal by rememberSaveable { mutableStateOf<String?>(null) }
    var fotoRemota by rememberSaveable { mutableStateOf(initial.fotoUrl) }
    var latitud by rememberSaveable { mutableStateOf(initial.latitud) }
    var longitud by rememberSaveable { mutableStateOf(initial.longitud) }
    var ubicacionMensaje by rememberSaveable { mutableStateOf<String?>(null) }
    
    val context = LocalContext.current
    val tomarFoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (!ok) fotoLocal = null
    }
    val pedirCamara = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) return@rememberLauncherForActivityResult
        val file = File(context.cacheDir, "relevamiento-${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        fotoLocal = uri.toString()
        tomarFoto.launch(uri)
    }
    val elegirFoto = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
        if (uri != null) fotoLocal = uri.toString()
    }
    val pedirUbicacion = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            capturarUbicacion(context) { ubicacion ->
                if (ubicacion == null) {
                    ubicacionMensaje = "No se pudo obtener la ubicación"
                } else {
                    latitud = ubicacion.latitud
                    longitud = ubicacion.longitud
                    ubicacionMensaje = null
                }
            }
        } else {
            ubicacionMensaje = "Se necesita permiso de ubicación"
        }
    }
    
    val error = (state as? RelevamientoFormUiState.Error)?.error
    val isLoading = state is RelevamientoFormUiState.Loading

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = fecha)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { fecha = it }
                    showDatePicker = false
                    onInputChanged()
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(if (initial.relevamientoId == null) "Nuevo relevamiento" else "Editar relevamiento") },
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
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
            ) {
                Button(
                    onClick = {
                        val bytes = fotoLocal?.let { uri ->
                            context.contentResolver.openInputStream(Uri.parse(uri))?.use { it.readBytes() }
                        }
                        val lat = latitud
                        val lng = longitud
                        val ubicacion = if (lat != null && lng != null) Ubicacion(lat, lng) else null
                        onSave(
                            RelevamientoDraft(fecha = fecha, nota = nota, fotoUrl = fotoRemota, ubicacion = ubicacion),
                            bytes
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
                        Text("Guardar Relevamiento")
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
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Sección: Fecha
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Fecha", style = MaterialTheme.typography.titleMedium)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { showDatePicker = true }
                ) {
                    OutlinedTextField(
                        value = formatFecha(fecha),
                        onValueChange = {},
                        readOnly = true,
                        enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                        trailingIcon = {
                            Icon(Icons.Default.DateRange, contentDescription = "Seleccionar fecha")
                        },
                        isError = error == RelevamientoError.FechaInvalida,
                        supportingText = {
                            if (error == RelevamientoError.FechaInvalida) Text(error.asMessage())
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Sección: Foto
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Evidencia Fotográfica", style = MaterialTheme.typography.titleMedium)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val vistaPrevia = fotoLocal ?: fotoRemota
                        if (vistaPrevia != null) {
                            AsyncImage(
                                model = vistaPrevia,
                                contentDescription = "Foto del relevamiento",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                OutlinedButton(onClick = { pedirCamara.launch(Manifest.permission.CAMERA) }) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Cambiar")
                                }
                                OutlinedButton(onClick = {
                                    fotoLocal = null
                                    fotoRemota = null
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Quitar")
                                }
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.AddAPhoto,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No hay foto adjunta", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Button(onClick = { pedirCamara.launch(Manifest.permission.CAMERA) }) {
                                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Cámara")
                                }
                                FilledTonalButton(onClick = { elegirFoto.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly)) }) {
                                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Galería")
                                }
                            }
                        }
                    }
                }
            }

            // Sección: Ubicación
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Ubicación", style = MaterialTheme.typography.titleMedium)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        if (latitud != null && longitud != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Ubicación guardada", style = MaterialTheme.typography.bodyLarge)
                                    Text(
                                        "$latitud, $longitud",
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
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.LocationOff, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    "Sin ubicación",
                                    modifier = Modifier.weight(1f),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = { pedirUbicacion.launch(Manifest.permission.ACCESS_FINE_LOCATION) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(if (latitud != null) "Actualizar ubicación" else "Capturar ubicación")
                        }
                        if (ubicacionMensaje != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = ubicacionMensaje!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            // Sección: Nota
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Nota u Observaciones", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = nota,
                    onValueChange = { nota = it; onInputChanged() },
                    placeholder = { Text("Escribe una descripción del avance o problema...") },
                    isError = error == RelevamientoError.NotaVacia,
                    supportingText = {
                        if (error == RelevamientoError.NotaVacia) Text(error.asMessage())
                    },
                    minLines = 4,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Otros Errores (generales)
            if (
                error != null &&
                error != RelevamientoError.NotaVacia &&
                error != RelevamientoError.FechaInvalida
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

private fun startOfTodayUtc(): Long {
    val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
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
