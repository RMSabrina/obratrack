package com.unaj.appmoviles.features.obras.relevamiento

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.unaj.appmoviles.features.obras.formatFecha
import com.unaj.appmoviles.obratrack.core.domain.model.Ubicacion
import android.Manifest
import android.content.Context
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Looper
import java.io.File
import com.unaj.appmoviles.obratrack.core.domain.error.RelevamientoError
import com.unaj.appmoviles.obratrack.core.domain.model.RelevamientoDraft
import com.unaj.appmoviles.obratrack.core.domain.ui.asMessage
import java.util.Calendar
import java.util.TimeZone

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
    viewModel: RelevamientoFormViewModel,
    initial: RelevamientoFormInitial,
    onBack: () -> Unit,
    onSaved: () -> Unit
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
                    TextButton(onClick = onBack) { Text("Volver") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = formatFecha(fecha),
                onValueChange = {},
                readOnly = true,
                label = { Text("Fecha") },
                isError = error == RelevamientoError.FechaInvalida,
                supportingText = {
                    if (error == RelevamientoError.FechaInvalida) Text(error.asMessage())
                },
                modifier = Modifier.fillMaxWidth()
            )
            TextButton(onClick = { showDatePicker = true }) { Text("Cambiar fecha") }
            val vistaPrevia = fotoLocal ?: fotoRemota
            if (vistaPrevia != null) {
                AsyncImage(
                    model = vistaPrevia,
                    contentDescription = "Foto del relevamiento",
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { pedirCamara.launch(Manifest.permission.CAMERA) }) { Text("Cámara") }
                TextButton(onClick = {
                    elegirFoto.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
                }) { Text("Galería") }
            }
            TextButton(onClick = {
                pedirUbicacion.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }) { Text("Capturar ubicación") }
            if (latitud != null && longitud != null) {
                Text("Ubicación: $latitud, $longitud")
            }
            ubicacionMensaje?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            OutlinedTextField(
                value = nota,
                onValueChange = { nota = it; onInputChanged() },
                label = { Text("Nota") },
                isError = error == RelevamientoError.NotaVacia,
                supportingText = {
                    if (error == RelevamientoError.NotaVacia) Text(error.asMessage())
                },
                minLines = 4,
                modifier = Modifier.fillMaxWidth()
            )
            if (
                error != null &&
                error != RelevamientoError.NotaVacia &&
                error != RelevamientoError.FechaInvalida
            ) {
                Text(error.asMessage(), color = MaterialTheme.colorScheme.error)
            }
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
                    CircularProgressIndicator(strokeWidth = 2.dp)
                } else {
                    Text("Guardar")
                }
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
