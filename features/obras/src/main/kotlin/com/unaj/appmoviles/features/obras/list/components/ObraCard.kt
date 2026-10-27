package com.unaj.appmoviles.features.obras.list.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.unaj.appmoviles.features.obras.formatFecha
import com.unaj.appmoviles.features.obras.formatHora
import com.unaj.appmoviles.features.obras.label
import com.unaj.appmoviles.obras.R
import com.unaj.appmoviles.obratrack.core.domain.model.EstadoObra
import com.unaj.appmoviles.obratrack.core.domain.model.Obra
import com.unaj.appmoviles.obratrack.core.ui.components.StatusBadge
import java.time.Clock

/**
 * Tarjeta de una obra. Las obras no tienen foto (las fotos son de los relevamientos), así que el
 * ancho completo es para el texto. [mostrarVisita] es false en las obras públicas: la visita es del dueño.
 */
@Composable
fun ObraCard(
    obra: Obra,
    onClick: () -> Unit,
    isFollowing: Boolean = false,
    followEnabled: Boolean = true,
    onFollowToggle: (() -> Unit)? = null,
    mostrarVisita: Boolean = true,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(
                start = 16.dp,
                top = 12.dp,
                bottom = 12.dp,
                end = if (onFollowToggle != null) 4.dp else 16.dp
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        text = obra.nombre,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    val (textColor, bgColor) = when (obra.estado) {
                        EstadoObra.ACTIVA -> MaterialTheme.colorScheme.onPrimaryContainer to MaterialTheme.colorScheme.primaryContainer
                        EstadoObra.PAUSADA -> MaterialTheme.colorScheme.onTertiaryContainer to MaterialTheme.colorScheme.tertiaryContainer
                        EstadoObra.FINALIZADA -> MaterialTheme.colorScheme.onSurfaceVariant to MaterialTheme.colorScheme.surfaceVariant
                    }
                    StatusBadge(
                        text = obra.estado.label(),
                        textColor = textColor,
                        backgroundColor = bgColor
                    )
                }
                if (obra.cliente.isNotBlank()) {
                    Text(
                        text = obra.cliente,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (obra.direccion.isNotBlank()) {
                    LineaConIcono(icono = Icons.Outlined.LocationOn) {
                        Text(
                            text = obra.direccion,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (mostrarVisita) {
                    LineaVisita(obra)
                }
            }
            if (onFollowToggle != null) {
                IconButton(onClick = onFollowToggle, enabled = followEnabled) {
                    Icon(
                        imageVector = if (isFollowing) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = stringResource(
                            if (isFollowing) R.string.obras_dejar_seguir else R.string.obras_seguir
                        ),
                        tint = if (isFollowing) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun LineaVisita(obra: Obra) {
    val context = LocalContext.current
    val visita = remember(obra.proximaVisita, obra.horaVisita) { visitaDeObra(obra, Clock.systemDefaultZone()) }
    val atrasada = (visita as? VisitaDeObra.Programada)?.atrasada == true
    val color = if (atrasada) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
    val texto = when (visita) {
        VisitaDeObra.SinVisita -> stringResource(R.string.obras_card_sin_visita)
        is VisitaDeObra.Programada -> {
            val fechaHora = visita.fechaConHora({ formatFecha(it) }, { formatHora(context, it) })
            stringResource(
                if (visita.atrasada) R.string.obras_card_visita_atrasada else R.string.obras_card_proxima_visita,
                fechaHora
            )
        }
    }
    LineaConIcono(icono = Icons.Outlined.CalendarMonth, tint = color) {
        Text(
            text = texto,
            style = MaterialTheme.typography.labelLarge,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun LineaConIcono(
    icono: ImageVector,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    contenido: @Composable () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(imageVector = icono, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
        contenido()
    }
}
