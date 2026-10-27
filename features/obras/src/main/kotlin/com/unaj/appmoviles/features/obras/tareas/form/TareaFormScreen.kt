package com.unaj.appmoviles.features.obras.tareas.form

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.unaj.appmoviles.features.obras.formatFecha
import com.unaj.appmoviles.features.obras.millisUtcToLocalDate
import com.unaj.appmoviles.features.obras.tareas.labelRes
import com.unaj.appmoviles.features.obras.toMillisUtc
import com.unaj.appmoviles.features.obras.rememberSalidaConConfirmacion
import com.unaj.appmoviles.obras.R
import com.unaj.appmoviles.obratrack.core.domain.error.TareaError
import com.unaj.appmoviles.obratrack.core.domain.model.CategoriaTarea
import com.unaj.appmoviles.obratrack.core.domain.model.EstadoTarea
import com.unaj.appmoviles.obratrack.core.domain.model.TareaDraft
import com.unaj.appmoviles.obratrack.core.domain.ui.LoadingContent
import com.unaj.appmoviles.obratrack.core.domain.ui.asMessage
import java.time.LocalDate

@Composable
fun TareaFormRoute(
    obraId: String,
    tareaId: String?,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: TareaFormViewModel = hiltViewModel(
        creationCallback = { factory: TareaFormViewModel.Factory -> factory.create(obraId, tareaId) }
    )
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onSaved()
    }
    TareaFormScreen(
        isNew = tareaId == null,
        state = state,
        onBack = onBack,
        onSave = viewModel::save,
        onInputChanged = viewModel::onInputChanged
    )
}

/** Si lo que hay en el formulario difiere de lo que tenía al abrirse. */
internal fun tareaTieneCambios(
    inicial: TareaDraft,
    titulo: String,
    descripcion: String,
    categoria: CategoriaTarea,
    estado: EstadoTarea,
    fechaInicio: LocalDate?,
    fechaFin: LocalDate?
): Boolean = titulo != inicial.titulo || descripcion != inicial.descripcion || categoria != inicial.categoria ||
    estado != inicial.estado || fechaInicio != inicial.fechaInicio || fechaFin != inicial.fechaFin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TareaFormScreen(
    isNew: Boolean,
    state: TareaFormUiState,
    onBack: () -> Unit,
    onSave: (TareaDraft) -> Unit,
    onInputChanged: () -> Unit,
    modifier: Modifier = Modifier
) {
    val inicial = state.inicial
    var hayCambios by rememberSaveable { mutableStateOf(false) }
    val salir = rememberSalidaConConfirmacion(hayCambios = hayCambios && !state.isSaving, onSalir = onBack)

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (isNew) R.string.tarea_nueva else R.string.tarea_editar)) },
                navigationIcon = {
                    IconButton(onClick = salir) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.volver))
                    }
                }
            )
        }
    ) { padding ->
        when {
            state.isLoading -> LoadingContent(Modifier.padding(padding))
            inicial == null -> Text(
                text = state.error?.asMessage().orEmpty(),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .padding(padding)
                    .padding(24.dp)
            )
            // El formulario se compone recién cuando hay valores iniciales,
            // así rememberSaveable arranca con los datos de la tarea a editar.
            else -> TareaFormContent(
                inicial = inicial,
                state = state,
                onSave = onSave,
                onInputChanged = onInputChanged,
                onHayCambios = { hayCambios = it },
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TareaFormContent(
    inicial: TareaDraft,
    state: TareaFormUiState,
    onSave: (TareaDraft) -> Unit,
    onInputChanged: () -> Unit,
    onHayCambios: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var titulo by rememberSaveable { mutableStateOf(inicial.titulo) }
    var descripcion by rememberSaveable { mutableStateOf(inicial.descripcion) }
    var categoria by rememberSaveable { mutableStateOf(inicial.categoria) }
    var estado by rememberSaveable { mutableStateOf(inicial.estado) }
    var fechaInicio by rememberSaveable { mutableStateOf(inicial.fechaInicio) }
    var fechaFin by rememberSaveable { mutableStateOf(inicial.fechaFin) }
    var showInicioPicker by rememberSaveable { mutableStateOf(false) }
    var showFinPicker by rememberSaveable { mutableStateOf(false) }

    val error = state.error

    LaunchedEffect(titulo, descripcion, categoria, estado, fechaInicio, fechaFin) {
        onHayCambios(tareaTieneCambios(inicial, titulo, descripcion, categoria, estado, fechaInicio, fechaFin))
    }

    if (showInicioPicker) {
        FechaPickerDialog(
            inicial = fechaInicio ?: fechaFin,
            onDismiss = { showInicioPicker = false },
            onConfirm = {
                fechaInicio = it
                showInicioPicker = false
                onInputChanged()
            }
        )
    }
    if (showFinPicker) {
        FechaPickerDialog(
            inicial = fechaFin ?: fechaInicio,
            onDismiss = { showFinPicker = false },
            onConfirm = {
                fechaFin = it
                showFinPicker = false
                onInputChanged()
            }
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = titulo,
                onValueChange = { titulo = it; onInputChanged() },
                label = { Text(stringResource(R.string.tarea_campo_titulo)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
                isError = error == TareaError.TituloVacio,
                supportingText = {
                    if (error == TareaError.TituloVacio) Text(error.asMessage())
                },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = descripcion,
                onValueChange = { descripcion = it; onInputChanged() },
                label = { Text(stringResource(R.string.tarea_campo_descripcion)) },
                minLines = 3,
                maxLines = 6,
                modifier = Modifier.fillMaxWidth()
            )

            SelectorField(
                label = stringResource(R.string.tarea_campo_categoria),
                value = stringResource(categoria.labelRes()),
                opciones = CategoriaTarea.entries,
                opcionLabel = { stringResource(it.labelRes()) },
                onSelect = { categoria = it; onInputChanged() }
            )

            SelectorField(
                label = stringResource(R.string.tarea_campo_estado),
                value = stringResource(estado.labelRes()),
                opciones = EstadoTarea.entries,
                opcionLabel = { stringResource(it.labelRes()) },
                onSelect = { estado = it; onInputChanged() }
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.tarea_campo_fechas), style = MaterialTheme.typography.titleMedium)
                Text(
                    text = stringResource(R.string.tarea_fechas_ayuda),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReadOnlyField(
                        label = stringResource(R.string.tarea_campo_fecha_inicio),
                        value = fechaInicio?.let(::formatFecha).orEmpty(),
                        trailingIcon = Icons.Default.DateRange,
                        isError = error == TareaError.FechasIncompletas && fechaInicio == null,
                        onClick = { showInicioPicker = true },
                        modifier = Modifier.weight(1f)
                    )
                    ReadOnlyField(
                        label = stringResource(R.string.tarea_campo_fecha_fin),
                        value = fechaFin?.let(::formatFecha).orEmpty(),
                        trailingIcon = Icons.Default.DateRange,
                        isError = (error == TareaError.FechasIncompletas && fechaFin == null) ||
                            error == TareaError.FechaFinAnterior,
                        onClick = { showFinPicker = true },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (error == TareaError.FechasIncompletas || error == TareaError.FechaFinAnterior) {
                    Text(
                        text = error.asMessage(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (fechaInicio != null || fechaFin != null) {
                    OutlinedButton(
                        onClick = {
                            fechaInicio = null
                            fechaFin = null
                            onInputChanged()
                        }
                    ) {
                        Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.tarea_quitar_fechas))
                    }
                }
            }

            // Errores que no corresponden a un campo (red, permisos, etc.)
            if (
                error != null &&
                error != TareaError.TituloVacio &&
                error != TareaError.FechasIncompletas &&
                error != TareaError.FechaFinAnterior
            ) {
                Text(
                    text = error.asMessage(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        BottomAppBar(
            containerColor = MaterialTheme.colorScheme.surface,
            contentPadding = PaddingValues(16.dp)
        ) {
            Button(
                onClick = {
                    onSave(
                        TareaDraft(
                            titulo = titulo,
                            descripcion = descripcion,
                            categoria = categoria,
                            fechaInicio = fechaInicio,
                            fechaFin = fechaFin,
                            estado = estado
                        )
                    )
                },
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.tarea_guardando))
                } else {
                    Text(stringResource(R.string.tarea_guardar))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FechaPickerDialog(
    inicial: LocalDate?,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit
) {
    // El DatePicker trabaja con la medianoche UTC del día: siempre se convierte en UTC.
    val pickerState = rememberDatePickerState(initialSelectedDateMillis = inicial?.toMillisUtc())
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val millis = pickerState.selectedDateMillis
                if (millis != null) onConfirm(millisUtcToLocalDate(millis)) else onDismiss()
            }) { Text(stringResource(R.string.aceptar)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancelar)) }
        }
    ) {
        DatePicker(state = pickerState)
    }
}

@Composable
private fun <T> SelectorField(
    label: String,
    value: String,
    opciones: List<T>,
    opcionLabel: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Box(modifier = modifier.fillMaxWidth()) {
        ReadOnlyField(
            label = label,
            value = value,
            trailingIcon = Icons.Default.ArrowDropDown,
            isError = false,
            onClick = { expanded = true }
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            opciones.forEach { opcion ->
                DropdownMenuItem(
                    text = { Text(opcionLabel(opcion)) },
                    onClick = {
                        expanded = false
                        onSelect(opcion)
                    }
                )
            }
        }
    }
}

/** Campo de solo lectura que abre un selector al tocarlo (mismo patrón que la fecha del relevamiento). */
@Composable
private fun ReadOnlyField(
    label: String,
    value: String,
    trailingIcon: ImageVector,
    isError: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .clickable(role = Role.Button, onClick = onClick)
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            enabled = false,
            label = { Text(label) },
            isError = isError,
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledBorderColor = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledLabelColor = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            ),
            trailingIcon = { Icon(trailingIcon, contentDescription = null) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}
