package com.unaj.appmoviles.obratrack.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.unaj.appmoviles.obratrack.R

/**
 * Aviso de que no hay red. Va en el `bottomBar` del Scaffold, encima de la barra inferior (sin
 * taparla). Si la barra está oculta, deja lugar a la barra de navegación del sistema.
 */
@Composable
fun BannerSinConexion(visible: Boolean, conBarraInferior: Boolean, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(),
        exit = shrinkVertically(),
        modifier = modifier
    ) {
        Surface(
            color = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
        ) {
            Text(
                text = stringResource(R.string.sin_conexion_banner),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (conBarraInferior) Modifier else Modifier.navigationBarsPadding())
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }
    }
}
