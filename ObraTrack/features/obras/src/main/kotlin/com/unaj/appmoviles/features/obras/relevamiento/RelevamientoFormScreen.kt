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
import com.unaj.appmoviles.features.obras.formatFecha
import com.unaj.appmoviles.obratrack.core.domain.error.RelevamientoError
import com.unaj.appmoviles.obratrack.core.domain.model.RelevamientoDraft
import com.unaj.appmoviles.obratrack.core.domain.ui.asMessage
import java.util.Calendar
import java.util.TimeZone

data class RelevamientoFormInitial(
    val obraId: String,
    val relevamientoId: String?,
    val nota: String,
    val fechaMillis: Long
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
    onSave: (RelevamientoDraft) -> Unit,
    onInputChanged: () -> Unit,
    modifier: Modifier = Modifier
) {
    var nota by rememberSaveable { mutableStateOf(initial.nota) }
    var fecha by rememberSaveable {
        mutableLongStateOf(if (initial.fechaMillis > 0L) initial.fechaMillis else startOfTodayUtc())
    }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
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
                onClick = { onSave(RelevamientoDraft(fecha = fecha, nota = nota)) },
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
