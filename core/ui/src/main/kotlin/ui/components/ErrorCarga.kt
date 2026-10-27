package com.unaj.appmoviles.obratrack.core.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.unaj.appmoviles.obratrack.core.domain.error.CausaError
import com.unaj.appmoviles.obratrack.core.domain.error.DomainError
import com.unaj.appmoviles.obratrack.core.domain.error.causa
import com.unaj.appmoviles.obratrack.core.ui.R

/** Mensaje de una falla al cargar datos, según su causa (nunca el texto técnico de la excepción). */
@StringRes
internal fun CausaError.mensajeDeCarga(): Int = when (this) {
    CausaError.SIN_CONEXION -> R.string.carga_error_sin_conexion
    CausaError.SIN_PERMISO -> R.string.carga_error_sin_permiso
    CausaError.NO_ENCONTRADO -> R.string.carga_error_no_encontrado
    CausaError.TARDO_DEMASIADO -> R.string.carga_error_timeout
    CausaError.OTRA -> R.string.carga_error_otro
}

/**
 * Error al cargar los datos de una pantalla, con el botón "Reintentar". Usar esto en vez de
 * dejar la pantalla vacía. `compact` es para una sección dentro de una lista.
 */
@Composable
fun ErrorCarga(
    error: DomainError,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val causa = error.causa()
    val mensaje = stringResource(causa.mensajeDeCarga())
    val reintentar = stringResource(R.string.carga_error_reintentar)
    if (compact) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = mensaje,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
            OutlinedButton(onClick = onRetry) { Text(reintentar) }
        }
    } else {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyState(
                icon = when (causa) {
                    CausaError.SIN_CONEXION -> Icons.Default.CloudOff
                    CausaError.SIN_PERMISO -> Icons.Default.Lock
                    CausaError.NO_ENCONTRADO -> Icons.Default.SearchOff
                    CausaError.TARDO_DEMASIADO -> Icons.Default.Timer
                    CausaError.OTRA -> Icons.Default.ErrorOutline
                },
                title = stringResource(R.string.carga_error_titulo),
                description = mensaje,
                actionLabel = reintentar,
                onActionClick = onRetry
            )
        }
    }
}
