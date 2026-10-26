package com.unaj.appmoviles.features.obras.relevamiento

import android.annotation.SuppressLint
import android.Manifest
import android.content.Context
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.tasks.CancellationTokenSource
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.rememberCoroutineScope
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.launch
import com.unaj.appmoviles.features.obras.formatFecha
import com.unaj.appmoviles.obratrack.core.domain.error.RelevamientoError
import androidx.compose.ui.res.stringResource
import com.unaj.appmoviles.obras.R
import com.unaj.appmoviles.obratrack.core.domain.model.RelevamientoDraft
import com.unaj.appmoviles.obratrack.core.domain.model.Ubicacion
import com.unaj.appmoviles.obratrack.core.ui.components.MapPickerDialog
import com.unaj.appmoviles.obratrack.core.ui.components.MapPreviewCard
import com.unaj.appmoviles.obratrack.core.domain.ui.asMessage
import java.io.File
import androidx.hilt.navigation.compose.hiltViewModel

data class RelevamientoFormInitial(
    val obraId: String,
    val relevamientoId: String?,
    val nota: String,
    val fechaMillis: Long,
    val fotoUrl: String? = null,
    val latitud: Double? = null,
    val longitud: Double? = null,
    val proximaVisita: Long? = null,
    val tareaId: String? = null
)

@Composable
fun RelevamientoFormRoute(
    initial: RelevamientoFormInitial,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: RelevamientoFormViewModel = hiltViewModel(
        creationCallback = { factory: RelevamientoFormViewModel.Factory -> 
            factory.create(initial.obraId, initial.relevamientoId, initial.tareaId)
        }
    )
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val selector by viewModel.selector.collectAsStateWithLifecycle()
    LaunchedEffect(state) {
        if (state is RelevamientoFormUiState.Saved) onSaved()
    }
    RelevamientoFormScreen(
        initial = initial,
        state = state,
        onBack = onBack,
        onSave = viewModel::save,
        onContinueReset = viewModel::onContinueReset,
        onInputChanged = viewModel::onErrorShown,
        selector = selector,
        onTareaSeleccionada = viewModel::onTareaSeleccionada,
        hoyMillisUtc = viewModel.hoyMillisUtc
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RelevamientoFormScreen(
    initial: RelevamientoFormInitial,
    state: RelevamientoFormUiState,
    onBack: () -> Unit,
    onSave: (RelevamientoDraft, ByteArray?, Boolean) -> Unit,
    onContinueReset: () -> Unit,
    onInputChanged: () -> Unit,
    selector: SelectorTareaUiState,
    onTareaSeleccionada: (String?) -> Unit,
    hoyMillisUtc: Long,
    modifier: Modifier = Modifier
) {
    var nota by rememberSaveable { mutableStateOf(initial.nota) }
    var fecha by rememberSaveable {
        mutableLongStateOf(if (initial.fechaMillis > 0L) initial.fechaMillis else hoyMillisUtc)
    }
    val snackbarHostState = remember { SnackbarHostState() }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var proximaVisita by rememberSaveable { mutableStateOf(initial.proximaVisita) }
    var showProximaVisitaPicker by rememberSaveable { mutableStateOf(false) }
    var fotoLocal by rememberSaveable { mutableStateOf<String?>(null) }
    var fotoRemota by rememberSaveable { mutableStateOf(initial.fotoUrl) }
    var latitud by rememberSaveable { mutableStateOf(initial.latitud) }
    var longitud by rememberSaveable { mutableStateOf(initial.longitud) }
    var showMapPicker by rememberSaveable { mutableStateOf(false) }
    var ubicacionMensaje by rememberSaveable { mutableStateOf<String?>(null) }
    var isCapturingLocation by remember { mutableStateOf(false) }
    var cancellationTokenSource by remember { mutableStateOf<CancellationTokenSource?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            cancellationTokenSource?.cancel()
        }
    }
    
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var pendingPhotoPath by rememberSaveable { mutableStateOf<String?>(null) }

    val tomarFoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok && pendingPhotoPath != null) {
            val file = File(pendingPhotoPath!!)
            if (file.exists() && file.length() > 0) {
                fotoLocal = file.absolutePath
                fotoRemota = null
                onInputChanged()
            }
        } else {
            pendingPhotoPath = null
        }
    }

    val launchCamera: () -> Unit = {
        val file = File(context.cacheDir, "relevamiento-${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        pendingPhotoPath = file.absolutePath
        tomarFoto.launch(uri)
    }

    val pedirCamara = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            launchCamera()
        }
    }

    val abrirCamara: () -> Unit = {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            launchCamera()
        } else {
            pedirCamara.launch(Manifest.permission.CAMERA)
        }
    }

    val elegirFoto = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
        if (uri != null) {
            fotoLocal = uri.toString()
            fotoRemota = null
            onInputChanged()
        }
    }

    fun readPhotoBytes(pathOrUri: String?): ByteArray? {
        if (pathOrUri.isNullOrBlank()) return null
        return try {
            if (pathOrUri.startsWith("content://")) {
                context.contentResolver.openInputStream(Uri.parse(pathOrUri))?.use { it.readBytes() }
            } else {
                val file = File(pathOrUri.removePrefix("file://"))
                if (file.exists() && file.length() > 0) {
                    file.readBytes()
                } else {
                    context.contentResolver.openInputStream(Uri.parse(pathOrUri))?.use { it.readBytes() }
                }
            }
        } catch (e: Exception) {
            null
        }
    }
    val pedirUbicacion = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            ubicacionMensaje = null
            isCapturingLocation = true
            cancellationTokenSource = capturarUbicacion(context) { ubicacion ->
                isCapturingLocation = false
                cancellationTokenSource = null
                if (ubicacion == null) {
                    ubicacionMensaje = context.getString(R.string.obras_error_ubicacion_no_disponible)
                } else {
                    latitud = ubicacion.latitud
                    longitud = ubicacion.longitud
                }
            }
        } else {
            isCapturingLocation = false
            ubicacionMensaje = context.getString(R.string.obras_error_permiso_ubicacion)
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
                }) { Text(stringResource(R.string.aceptar)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.cancelar)) }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    if (showProximaVisitaPicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = proximaVisita ?: (System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000)
        )
        DatePickerDialog(
            onDismissRequest = { showProximaVisitaPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { proximaVisita = it }
                    showProximaVisitaPicker = false
                    onInputChanged()
                }) { Text(stringResource(R.string.aceptar)) }
            },
            dismissButton = {
                TextButton(onClick = { showProximaVisitaPicker = false }) { Text(stringResource(R.string.cancelar)) }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    val mensajeGuardadoContinuar = stringResource(R.string.relevamiento_form_guardado_continuar)
    LaunchedEffect(state) {
        if (state is RelevamientoFormUiState.SavedAndContinue) {
            fotoLocal = null
            fotoRemota = null
            nota = ""
            pendingPhotoPath = null
            coroutineScope.launch {
                scrollState.animateScrollTo(0)
            }
            snackbarHostState.showSnackbar(
                message = mensajeGuardadoContinuar,
                duration = androidx.compose.material3.SnackbarDuration.Short
            )
            onContinueReset()
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (initial.relevamientoId == null) R.string.relevamiento_form_nuevo else R.string.relevamiento_form_editar)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.volver))
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
            ) {
                if (initial.relevamientoId == null) {
                    val hasPendingContent = fotoLocal != null || nota.isNotBlank()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val bytes = readPhotoBytes(fotoLocal)
                                val lat = latitud
                                val lng = longitud
                                val ubicacion = if (lat != null && lng != null) Ubicacion(lat, lng) else null
                                onSave(
                                    RelevamientoDraft(fecha = fecha, nota = nota, fotoUrl = fotoRemota, ubicacion = ubicacion, proximaVisita = proximaVisita),
                                    bytes,
                                    true
                                )
                            },
                            enabled = !isLoading && hasPendingContent,
                            modifier = Modifier.weight(1.2f)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(18.dp),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.guardando))
                            } else {
                                Icon(Icons.Default.AddAPhoto, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.relevamiento_form_guardar_y_otra), maxLines = 1)
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                if (hasPendingContent) {
                                    val bytes = readPhotoBytes(fotoLocal)
                                    val lat = latitud
                                    val lng = longitud
                                    val ubicacion = if (lat != null && lng != null) Ubicacion(lat, lng) else null
                                    onSave(
                                        RelevamientoDraft(fecha = fecha, nota = nota, fotoUrl = fotoRemota, ubicacion = ubicacion, proximaVisita = proximaVisita),
                                        bytes,
                                        false
                                    )
                                } else {
                                    onBack()
                                }
                            },
                            enabled = !isLoading,
                            modifier = Modifier.weight(0.8f)
                        ) {
                            Icon(
                                imageVector = if (hasPendingContent) Icons.Default.Check else Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(if (hasPendingContent) R.string.relevamiento_form_finalizar else R.string.relevamiento_form_salir), maxLines = 1)
                        }
                    }
                } else {
                    Button(
                        onClick = {
                            val bytes = readPhotoBytes(fotoLocal)
                            val lat = latitud
                            val lng = longitud
                            val ubicacion = if (lat != null && lng != null) Ubicacion(lat, lng) else null
                            onSave(
                                RelevamientoDraft(fecha = fecha, nota = nota, fotoUrl = fotoRemota, ubicacion = ubicacion, proximaVisita = proximaVisita),
                                bytes,
                                false
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
                            Text(stringResource(R.string.guardando))
                        } else {
                            Text(stringResource(R.string.relevamiento_form_guardar_cambios))
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Sección: Fecha
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.relevamiento_form_fecha), style = MaterialTheme.typography.titleMedium)
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
                            Icon(Icons.Default.DateRange, contentDescription = stringResource(R.string.relevamiento_form_seleccionar_fecha))
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
                Text(stringResource(R.string.relevamiento_form_evidencia), style = MaterialTheme.typography.titleMedium)
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
                            SubcomposeAsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(
                                        when {
                                            vistaPrevia.startsWith("/") -> File(vistaPrevia)
                                            else -> Uri.parse(vistaPrevia)
                                        }
                                    )
                                    .crossfade(true)
                                    .build(),
                                contentDescription = stringResource(R.string.relevamiento_form_foto),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(240.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop,
                                loading = {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(MaterialTheme.colorScheme.surfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                                    }
                                },
                                error = {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                imageVector = Icons.Default.Warning,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                text = stringResource(R.string.relevamiento_form_error_vista_previa),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                OutlinedButton(onClick = abrirCamara) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(stringResource(R.string.relevamiento_form_cambiar))
                                }
                                OutlinedButton(onClick = {
                                    fotoLocal = null
                                    fotoRemota = null
                                    pendingPhotoPath = null
                                    onInputChanged()
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(stringResource(R.string.relevamiento_form_quitar))
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
                            Text(stringResource(R.string.relevamiento_form_sin_foto), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Button(onClick = abrirCamara) {
                                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(stringResource(R.string.relevamiento_form_camara))
                                }
                                FilledTonalButton(onClick = { elegirFoto.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly)) }) {
                                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(stringResource(R.string.relevamiento_form_galeria))
                                }
                            }
                        }
                    }
                }
            }

            // Sección: Ubicación
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.relevamiento_form_ubicacion), style = MaterialTheme.typography.titleMedium)
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
                                        Text(stringResource(R.string.obras_borrar_ubicacion))
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
                        Spacer(modifier = Modifier.height(16.dp))
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
                                    Text(stringResource(R.string.obras_capturando_ubicacion))
                                } else {
                                    Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(if (latitud != null) stringResource(R.string.obras_actualizar_ubicacion) else stringResource(R.string.obras_capturar_ubicacion))
                                }
                            }

                            OutlinedButton(
                                onClick = { showMapPicker = true },
                                enabled = !isCapturingLocation,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.obras_ajustar_en_mapa))
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

            // Sección: Planificación de Próxima Visita (Recordatorio)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(R.string.relevamiento_form_proxima_visita),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (proximaVisita != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = stringResource(R.string.relevamiento_form_programada_para, formatFecha(proximaVisita!!)),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            IconButton(
                                onClick = {
                                    proximaVisita = null
                                    onInputChanged()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = stringResource(R.string.relevamiento_form_quitar_fecha),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        OutlinedButton(
                            onClick = { showProximaVisitaPicker = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.EditCalendar, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.relevamiento_form_cambiar_proxima))
                        }
                    } else {
                        Text(
                            text = stringResource(R.string.relevamiento_form_proxima_ayuda),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(
                            onClick = { showProximaVisitaPicker = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.AddAlert, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.relevamiento_form_programar_proxima))
                        }
                    }
                }
            }

            // Sección: Nota
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.relevamiento_form_nota), style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = nota,
                    onValueChange = { nota = it; onInputChanged() },
                    placeholder = { Text(stringResource(R.string.relevamiento_form_nota_placeholder)) },
                    isError = error == RelevamientoError.NotaVacia,
                    supportingText = {
                        if (error == RelevamientoError.NotaVacia) Text(error.asMessage())
                    },
                    minLines = 4,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Sección: Tarea vinculada (opcional, solo el dueño)
            TareaSelector(
                selector = selector,
                onTareaSeleccionada = onTareaSeleccionada,
                enabled = !isLoading
            )

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
