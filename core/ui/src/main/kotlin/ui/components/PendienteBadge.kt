package com.unaj.appmoviles.obratrack.core.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.unaj.appmoviles.obratrack.core.ui.R

/** Marca "sin sincronizar": hay cambios guardados en el teléfono que el servidor aún no confirmó. */
@Composable
fun PendienteBadge(modifier: Modifier = Modifier) {
    StatusBadge(
        text = stringResource(R.string.sync_pendiente),
        textColor = MaterialTheme.colorScheme.onSecondaryContainer,
        backgroundColor = MaterialTheme.colorScheme.secondaryContainer,
        modifier = modifier
    )
}
