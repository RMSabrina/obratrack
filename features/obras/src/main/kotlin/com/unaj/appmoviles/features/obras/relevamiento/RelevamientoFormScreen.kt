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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.FactCheck
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.unaj.appmoviles.features.obras.formatFecha
import com.unaj.appmoviles.obratrack.core.domain.error.RelevamientoError
import com.unaj.appmoviles.obras.R
import com.unaj.appmoviles.obratrack.core.domain.model.RelevamientoDraft
import com.unaj.appmoviles.obratrack.core.domain.model.TipoRelevamiento
import com.unaj.appmoviles.obratrack.core.domain.model.Ubicacion
import com.unaj.appmoviles.obratrack.core.ui.components.MapPickerDialog
import com.unaj.appmoviles.obratrack.core.ui.components.MapPreviewCard
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
    val longitud: Double? = null,
    val proximaVisita: Long? = null,
    val tipo: String = TipoRelevamiento.AVANCE,
    val checklist: List<String> = emptyList()
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    var tipo by rememberSaveable { mutableStateOf(initial.tipo.ifBlank { TipoRelevamiento.AVANCE }) }
    var selectedChecklist by rememberSaveable { mutableStateOf(initial.checklist.toSet()) }
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
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
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
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { showProximaVisitaPicker = false }) { Text("Cancelar") }
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
                            RelevamientoDraft(
                                fecha = fecha,
                                nota = nota,
                                fotoUrl = fotoRemota,
                                ubicacion = ubicacion,
                                proximaVisita = proximaVisita,
                                tipo = tipo,
                                checklist = selectedChecklist.toList()
                            ),
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
                .imePadding()
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

            // Sección: Tipo de Relevamiento / Auditoría
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
                            imageVector = Icons.Default.Category,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                text = "Tipo de Inspección",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Define el objetivo del relevamiento",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TipoRelevamiento.TODOS.forEach { item ->
                            val selected = tipo == item
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    tipo = item
                                    onInputChanged()
                                },
                                label = { Text(item) },
                                leadingIcon = if (selected) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null
                            )
                        }
                    }
                }
            }

            // Sección: Checklist de Conformidad en Terreno
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
                            imageVector = Icons.Default.FactCheck,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                text = "Checklist de Conformidad",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Controles verificados conformes en sitio",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    TipoRelevamiento.CHECKLIST_DEFAULT.forEach { item ->
                        val checked = selectedChecklist.contains(item)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    selectedChecklist = if (checked) {
                                        selectedChecklist - item
                                    } else {
                                        selectedChecklist + item
                                    }
                                    onInputChanged()
                                }
                                .padding(vertical = 4.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = item,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 12.dp)
                            )
                            Switch(
                                checked = checked,
                                onCheckedChange = { isChecked ->
                                    selectedChecklist = if (isChecked) {
                                        selectedChecklist + item
                                    } else {
                                        selectedChecklist - item
                                    }
                                    onInputChanged()
                                }
                            )
                        }
                    }
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
                Text("Ubicación del Relevamiento", style = MaterialTheme.typography.titleMedium)
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
                                title = "Ubicación registrada"
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOff,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Sin ubicación registrada",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val permiso = Manifest.permission.ACCESS_FINE_LOCATION
                                    pedirUbicacion.launch(permiso)
                                },
                                enabled = !isCapturingLocation,
                                modifier = Modifier.weight(1f)
                            ) {
                                if (isCapturingLocation) {
                                    CircularProgressIndicator(
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(18.dp),
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("Obteniendo...")
                                } else {
                                    Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Obtener GPS")
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
                            text = "Próxima Visita (Recordatorio)",
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
                                text = "Programada para: " + formatFecha(proximaVisita!!),
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
                                    contentDescription = "Quitar fecha",
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
                            Text("Cambiar fecha de próxima visita")
                        }
                    } else {
                        Text(
                            text = "Opcional: define cuándo debería ser el próximo relevamiento para recibir un recordatorio automático en el dispositivo.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(
                            onClick = { showProximaVisitaPicker = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.AddAlert, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Programar próxima visita")
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
