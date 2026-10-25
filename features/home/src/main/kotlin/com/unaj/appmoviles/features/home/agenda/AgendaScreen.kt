package com.unaj.appmoviles.features.home.agenda

import androidx.activity.compose.BackHandler
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.EditCalendar
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unaj.appmoviles.features.obras.formatFecha
import com.unaj.appmoviles.obratrack.core.domain.model.CategoriaAgendaVisita
import com.unaj.appmoviles.obratrack.core.domain.model.ItemAgendaVisita
import com.unaj.appmoviles.obratrack.core.domain.model.Obra
import com.unaj.appmoviles.obratrack.core.domain.ui.LoadingContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgendaScreen(
    state: AgendaUiState,
    onBack: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToObras: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onOpenObra: (String) -> Unit,
    onIniciarRelevamiento: (String) -> Unit,
    onFiltroChanged: (FiltroAgenda) -> Unit,
    onIniciarReprogramacion: (Obra) -> Unit,
    onCancelarReprogramacion: () -> Unit,
    onConfirmarReprogramacion: (Obra, Long?) -> Unit,
    onClearFeedbackMessage: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.mensajeFeedback) {
        state.mensajeFeedback?.let {
            snackbarHostState.showSnackbar(it)
            onClearFeedbackMessage()
        }
    }

    if (state.obraParaReprogramar != null) {
        val obra = state.obraParaReprogramar
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = obra.proximaVisita ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = onCancelarReprogramacion,
            confirmButton = {
                Button(
                    onClick = {
                        onConfirmarReprogramacion(obra, pickerState.selectedDateMillis)
                    }
                ) {
                    Text("Guardar Fecha")
                }
            },
            dismissButton = {
                Row {
                    if (obra.proximaVisita != null) {
                        TextButton(
                            onClick = {
                                onConfirmarReprogramacion(obra, null)
                            }
                        ) {
                            Text("Quitar fecha", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    TextButton(onClick = onCancelarReprogramacion) {
                        Text("Cancelar")
                    }
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Agenda de Visitas",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Itinerario y control técnico de campo",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                }
            )
        },
        bottomBar = {
            AgendaBottomNavigation(
                onNavigateToHome = onNavigateToHome,
                onNavigateToObras = onNavigateToObras,
                onNavigateToProfile = onNavigateToProfile
            )
        }
    ) { padding ->
        if (state.isLoading) {
            LoadingContent(Modifier.padding(padding).fillMaxSize())
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    // Métricas ejecutivas de la agenda
                    ResumenAgendaHeader(
                        totalHoy = state.totalHoy,
                        totalVencidas = state.totalVencidas,
                        totalEstaSemana = state.totalEstaSemana,
                        totalSinProgramar = state.totalSinProgramar
                    )
                }

                item {
                    // Filtros horizontales
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FiltroAgenda.entries.forEach { filtro ->
                            FilterChip(
                                selected = state.filtro == filtro,
                                onClick = { onFiltroChanged(filtro) },
                                label = {
                                    val count = when (filtro) {
                                        FiltroAgenda.TODAS -> state.items.size
                                        FiltroAgenda.URGENTES_Y_HOY -> state.totalHoy + state.totalVencidas
                                        FiltroAgenda.ESTA_SEMANA -> state.totalEstaSemana
                                        FiltroAgenda.PROXIMAS -> state.items.count { it.categoria == CategoriaAgendaVisita.PROXIMAS }
                                        FiltroAgenda.SIN_PROGRAMAR -> state.totalSinProgramar
                                    }
                                    Text("${filtro.label} ($count)")
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }

                if (state.itemsFiltrados.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Event,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    text = "No hay visitas para este filtro",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Tenés todo al día o podés seleccionar 'Todas' para ver el itinerario completo.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(state.itemsFiltrados, key = { it.obra.id }) { itemAgenda ->
                        AgendaVisitaCard(
                            item = itemAgenda,
                            onOpenObra = { onOpenObra(itemAgenda.obra.id) },
                            onIniciarRelevamiento = { onIniciarRelevamiento(itemAgenda.obra.id) },
                            onReprogramar = { onIniciarReprogramacion(itemAgenda.obra) },
                            onComoLlegar = {
                                abrirMapa(
                                    context = context,
                                    direccion = itemAgenda.obra.direccion,
                                    latitud = itemAgenda.obra.ubicacion?.latitud,
                                    longitud = itemAgenda.obra.ubicacion?.longitud
                                )
                            }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun ResumenAgendaHeader(
    totalHoy: Int,
    totalVencidas: Int,
    totalEstaSemana: Int,
    totalSinProgramar: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MetricaAgendaCard(
            label = "Hoy",
            valor = totalHoy.toString(),
            colorFondo = if (totalHoy > 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
            colorTexto = if (totalHoy > 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        MetricaAgendaCard(
            label = "Atrasadas",
            valor = totalVencidas.toString(),
            colorFondo = if (totalVencidas > 0) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surface,
            colorTexto = if (totalVencidas > 0) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        MetricaAgendaCard(
            label = "Esta semana",
            valor = totalEstaSemana.toString(),
            colorFondo = MaterialTheme.colorScheme.surface,
            colorTexto = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        MetricaAgendaCard(
            label = "Sin fecha",
            valor = totalSinProgramar.toString(),
            colorFondo = if (totalSinProgramar > 0) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surface,
            colorTexto = if (totalSinProgramar > 0) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun MetricaAgendaCard(
    label: String,
    valor: String,
    colorFondo: Color,
    colorTexto: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = colorFondo),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = valor,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = colorTexto
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = colorTexto.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun AgendaVisitaCard(
    item: ItemAgendaVisita,
    onOpenObra: () -> Unit,
    onIniciarRelevamiento: () -> Unit,
    onReprogramar: () -> Unit,
    onComoLlegar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = when (item.categoria) {
        CategoriaAgendaVisita.URGENTE_VENCIDA -> MaterialTheme.colorScheme.error
        CategoriaAgendaVisita.HOY -> MaterialTheme.colorScheme.primary
        CategoriaAgendaVisita.ESTA_SEMANA -> MaterialTheme.colorScheme.secondary
        CategoriaAgendaVisita.PROXIMAS -> MaterialTheme.colorScheme.outline
        CategoriaAgendaVisita.SIN_PROGRAMAR -> MaterialTheme.colorScheme.tertiary
    }

    val badgeText = when (item.categoria) {
        CategoriaAgendaVisita.URGENTE_VENCIDA -> "🚨 ATRASADA (${Math.abs(item.diasDiferencia)} DÍAS)"
        CategoriaAgendaVisita.HOY -> "🔔 VISITA PARA HOY"
        CategoriaAgendaVisita.ESTA_SEMANA -> "📅 EN ${item.diasDiferencia} DÍAS (${item.fechaProgramadaMillis?.let { formatFecha(it) }})"
        CategoriaAgendaVisita.PROXIMAS -> "⏳ ${item.fechaProgramadaMillis?.let { formatFecha(it) }}"
        CategoriaAgendaVisita.SIN_PROGRAMAR -> "⚠️ SIN FECHA (${item.diasSinVisita ?: 0} DÍAS SIN RELEVAMIENTO)"
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header: Badge de estado + Botón ver obra
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = accentColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                TextButton(
                    onClick = onOpenObra,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Ver Obra",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            // Nombre y comitente
            Column {
                Text(
                    text = item.obra.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (item.obra.cliente.isNotBlank()) {
                    Text(
                        text = "Cliente: ${item.obra.cliente}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Dirección
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = item.obra.direccion.ifBlank { "Sin dirección especificada" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Última visita
            val ultima = item.ultimaVisitaMillis
            if (ultima != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Último relevamiento: ${formatFecha(ultima)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Botones de acción operativa
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Iniciar Relevamiento
                Button(
                    onClick = onIniciarRelevamiento,
                    modifier = Modifier.weight(1.3f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Relevar",
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                // Cómo llegar / Mapa
                OutlinedButton(
                    onClick = onComoLlegar,
                    modifier = Modifier.weight(1.1f),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DirectionsCar,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Mapa",
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                // Reprogramar
                OutlinedButton(
                    onClick = onReprogramar,
                    modifier = Modifier.weight(1.1f),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.EditCalendar,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (item.fechaProgramadaMillis != null) "Cambiar" else "Agendar",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

@Composable
fun AgendaBottomNavigation(
    onNavigateToHome: () -> Unit,
    onNavigateToObras: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.primary
    ) {
        NavigationBarItem(
            selected = false,
            onClick = onNavigateToHome,
            icon = { Icon(Icons.Outlined.Home, contentDescription = "Inicio") },
            label = { Text("Inicio") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
        NavigationBarItem(
            selected = false,
            onClick = onNavigateToObras,
            icon = { Icon(Icons.Outlined.Business, contentDescription = "Obras") },
            label = { Text("Obras") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
        NavigationBarItem(
            selected = true,
            onClick = { },
            icon = { Icon(Icons.Outlined.CalendarMonth, contentDescription = "Agenda") },
            label = { Text("Agenda", fontWeight = FontWeight.Bold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
        NavigationBarItem(
            selected = false,
            onClick = onNavigateToProfile,
            icon = { Icon(Icons.Outlined.Person, contentDescription = "Perfil") },
            label = { Text("Perfil") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}

fun abrirMapa(context: Context, direccion: String, latitud: Double?, longitud: Double?) {
    val uri = if (latitud != null && longitud != null) {
        Uri.parse("geo:$latitud,$longitud?q=$latitud,$longitud(${Uri.encode(direccion.ifBlank { "Obra" })})")
    } else {
        Uri.parse("geo:0,0?q=${Uri.encode(direccion)}")
    }
    val mapIntent = Intent(Intent.ACTION_VIEW, uri).apply {
        setPackage("com.google.android.apps.maps")
    }
    try {
        context.startActivity(mapIntent)
    } catch (_: Exception) {
        val fallbackIntent = Intent(Intent.ACTION_VIEW, uri)
        try {
            context.startActivity(fallbackIntent)
        } catch (_: Exception) {
            // Manejado silenciosamente si no hay app instalada
        }
    }
}
