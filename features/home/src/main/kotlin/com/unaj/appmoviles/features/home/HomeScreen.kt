package com.unaj.appmoviles.features.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.material.icons.outlined.Event
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.TextButton
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.unaj.appmoviles.features.home.components.RelevamientoCard
import com.unaj.appmoviles.home.R
import com.unaj.appmoviles.obratrack.core.domain.model.Relevamiento
import com.unaj.appmoviles.obratrack.core.domain.ui.LoadingContent
import com.unaj.appmoviles.obratrack.core.ui.components.ErrorCarga

import androidx.compose.material.icons.outlined.CalendarMonth
import com.unaj.appmoviles.features.obras.formatFecha
import com.unaj.appmoviles.obratrack.core.domain.model.CategoriaAgendaVisita

@Composable
fun HomeScreen(
    state: HomeUiState,
    onNavigateToObras: () -> Unit,
    onNavigateToAgenda: () -> Unit,
    onNavigateToObraSelection: () -> Unit,
    onNavigateToObrasPublicas: () -> Unit,
    onOpenRelevamiento: (Relevamiento) -> Unit,
    onRetry: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (state.isLoading) {
            LoadingContent(Modifier.padding(padding).fillMaxSize())
        } else if (state.error != null) {
            ErrorCarga(error = state.error, onRetry = onRetry, modifier = Modifier.padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 24.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Column(modifier = Modifier.padding(bottom = 8.dp)) {
                        // Marca: lo más chico y de color; el saludo es lo que domina.
                        Text(
                            text = "ObraTrack",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(28.dp))
                        Text(
                            text = stringResource(
                                R.string.home_saludo,
                                primerNombre(state.userName).ifBlank { stringResource(R.string.home_usuario) }
                            ),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.home_subtitulo),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // La alerta (si hay) y la acción principal van juntas: una franja suave y un solo botón de color pleno.
                item {
                    val destacada = state.proximaVisitaDestacada
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (state.totalVisitasAtrasadas > 0 && destacada != null) {
                            FranjaDeAtencion(
                                texto = pluralStringResource(
                                    R.plurals.home_atrasadas_resumen,
                                    state.totalVisitasAtrasadas,
                                    state.totalVisitasAtrasadas,
                                    destacada.obra.nombre
                                ),
                                icono = Icons.Outlined.Warning,
                                container = MaterialTheme.colorScheme.errorContainer,
                                content = MaterialTheme.colorScheme.onErrorContainer,
                                onClick = onNavigateToAgenda
                            )
                        } else if (destacada != null && destacada.categoria == CategoriaAgendaVisita.HOY) {
                            FranjaDeAtencion(
                                texto = pluralStringResource(
                                    R.plurals.home_hoy_resumen,
                                    state.totalVisitasHoy,
                                    state.totalVisitasHoy,
                                    destacada.obra.nombre
                                ),
                                icono = Icons.Outlined.Event,
                                container = MaterialTheme.colorScheme.primaryContainer,
                                content = MaterialTheme.colorScheme.onPrimaryContainer,
                                onClick = onNavigateToAgenda
                            )
                        }
                        AccionPrincipal(
                            title = stringResource(R.string.home_accion_nuevo_relevamiento),
                            onClick = onNavigateToObraSelection
                        )
                    }
                }

                item {
                    FilaObrasPublicas(onClick = onNavigateToObrasPublicas)
                }

                item {
                    // Más aire antes de la lista: separa las acciones de lo reciente.
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.home_relevamientos_recientes),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .weight(1f)
                                .semantics { heading() }
                        )
                        TextButton(onClick = onNavigateToObras) {
                            Text(stringResource(R.string.home_ver_todos))
                        }
                    }
                }

                if (state.recientes.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.home_sin_relevamientos),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    }
                } else {
                    items(state.recientes, key = { it.relevamiento.id }) { reciente ->
                        RelevamientoCard(
                            reciente = reciente,
                            onClick = { onOpenRelevamiento(reciente.relevamiento) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Aviso de una línea (dos con fuente grande), con el color suave del contenedor y no el pleno:
 * informa sin competir con la acción principal.
 */
@Composable
private fun FranjaDeAtencion(
    texto: String,
    icono: ImageVector,
    container: Color,
    content: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = container,
        contentColor = content
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 48.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(imageVector = icono, contentDescription = null, modifier = Modifier.size(20.dp))
            Text(
                text = texto,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Icon(imageVector = Icons.Outlined.ChevronRight, contentDescription = null, modifier = Modifier.size(20.dp))
        }
    }
}

/** La acción más importante de Home y el único elemento con color pleno: botón ancho y alto, un solo texto. */
@Composable
private fun AccionPrincipal(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        Icon(
            imageVector = Icons.Outlined.CameraAlt,
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

/** Acceso secundario: una fila liviana, sin tarjeta de fondo; el color queda en el ícono. */
@Composable
private fun FilaObrasPublicas(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.LocationOn,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = stringResource(R.string.home_accion_obras_publicas),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
