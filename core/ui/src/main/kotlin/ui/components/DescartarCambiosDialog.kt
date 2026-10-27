package com.unaj.appmoviles.obratrack.core.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.unaj.appmoviles.obratrack.core.ui.R

/** Pregunta si se descartan los cambios sin guardar de un formulario antes de salir. */
@Composable
fun DescartarCambiosDialog(onDescartar: () -> Unit, onSeguir: () -> Unit) {
    AlertDialog(
        onDismissRequest = onSeguir,
        title = { Text(stringResource(R.string.descartar_titulo)) },
        text = { Text(stringResource(R.string.descartar_mensaje)) },
        confirmButton = {
            TextButton(onClick = onDescartar) { Text(stringResource(R.string.descartar_confirmar)) }
        },
        dismissButton = {
            TextButton(onClick = onSeguir) { Text(stringResource(R.string.descartar_seguir)) }
        }
    )
}
