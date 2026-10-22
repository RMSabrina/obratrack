package com.unaj.appmoviles.features.obras.tareas.list.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.unaj.appmoviles.features.obras.formatFecha
import com.unaj.appmoviles.features.obras.tareas.labelRes
import com.unaj.appmoviles.obras.R
import com.unaj.appmoviles.obratrack.core.domain.model.EstadoTarea
import com.unaj.appmoviles.obratrack.core.ui.components.StatusBadge
import com.unaj.appmoviles.features.obras.tareas.list.TareaItem

@Composable
fun TareaCard(
    item: TareaItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tarea = item.tarea
    val (badgeBackground, badgeText) = when (tarea.estado) {
        EstadoTarea.PENDIENTE -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        EstadoTarea.EN_CURSO -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        EstadoTarea.COMPLETADA -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
    }

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = tarea.titulo,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                StatusBadge(
                    text = stringResource(tarea.estado.labelRes()),
                    backgroundColor = badgeBackground,
                    textColor = badgeText,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Text(
                text = stringResource(tarea.categoria.labelRes()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val inicio = tarea.fechaInicio
                val fin = tarea.fechaFin
                val color = if (item.atrasada) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                Icon(
                    imageVector = if (item.atrasada) Icons.Default.Warning else Icons.Default.DateRange,
                    contentDescription = null,
                    tint = color
                )
                Text(
                    text = if (inicio != null && fin != null) {
                        stringResource(R.string.tareas_rango_fechas, formatFecha(inicio), formatFecha(fin))
                    } else {
                        stringResource(R.string.tareas_sin_fechas)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = color,
                    modifier = Modifier.weight(1f)
                )
                if (item.atrasada) {
                    StatusBadge(
                        text = stringResource(R.string.tarea_atrasada),
                        backgroundColor = MaterialTheme.colorScheme.errorContainer,
                        textColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}
