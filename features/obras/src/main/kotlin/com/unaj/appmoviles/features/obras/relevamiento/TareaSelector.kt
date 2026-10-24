package com.unaj.appmoviles.features.obras.relevamiento

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.unaj.appmoviles.obras.R

/**
 * Selector opcional de la tarea vinculada. No se dibuja para un no dueño: [SelectorTareaUiState.disponible]
 * es false y las tareas ni se consultan.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TareaSelector(
    selector: SelectorTareaUiState,
    onTareaSeleccionada: (String?) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    if (!selector.disponible && !selector.error) return
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.relevamiento_tarea_titulo), style = MaterialTheme.typography.titleMedium)
        when {
            selector.error -> Text(
                text = stringResource(R.string.relevamiento_tarea_error),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
            selector.opciones.isEmpty() -> Text(
                text = stringResource(R.string.relevamiento_tarea_sin_tareas),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            else -> {
                var expanded by rememberSaveable { mutableStateOf(false) }
                val ninguna = stringResource(R.string.relevamiento_tarea_ninguna)
                val seleccionada = selector.opciones.find { it.id == selector.seleccionada }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { if (enabled) expanded = it }
                ) {
                    OutlinedTextField(
                        value = seleccionada?.let { opcionTexto(it) } ?: ninguna,
                        onValueChange = {},
                        readOnly = true,
                        enabled = enabled,
                        singleLine = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        supportingText = { Text(stringResource(R.string.relevamiento_tarea_ayuda)) },
                        modifier = Modifier
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DropdownMenuItem(
                            text = { Text(ninguna) },
                            onClick = {
                                onTareaSeleccionada(null)
                                expanded = false
                            }
                        )
                        // Limitación conocida: dos tareas con el mismo título se ven iguales.
                        selector.opciones.forEach { opcion ->
                            DropdownMenuItem(
                                text = { Text(opcionTexto(opcion)) },
                                onClick = {
                                    onTareaSeleccionada(opcion.id)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun opcionTexto(opcion: OpcionTarea): String =
    if (opcion.completada) {
        stringResource(R.string.relevamiento_tarea_completada, opcion.titulo)
    } else {
        opcion.titulo
    }
