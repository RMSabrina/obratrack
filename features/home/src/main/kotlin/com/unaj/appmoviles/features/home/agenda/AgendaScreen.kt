package com.unaj.appmoviles.features.home.agenda

import androidx.activity.compose.BackHandler
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
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
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.EditCalendar
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.HORA_VISITA_POR_DEFECTO_MINUTOS
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unaj.appmoviles.features.obras.formatFecha
import com.unaj.appmoviles.features.obras.formatHora
import com.unaj.appmoviles.home.R
import com.unaj.appmoviles.obratrack.core.domain.model.CategoriaAgendaVisita
import com.unaj.appmoviles.obratrack.core.domain.model.ItemAgendaVisita
import com.unaj.appmoviles.obratrack.core.domain.model.Obra
import com.unaj.appmoviles.obratrack.core.domain.ui.LoadingContent
import com.unaj.appmoviles.obratrack.core.ui.components.ErrorCarga
import com.unaj.appmoviles.obratrack.core.domain.ui.asMessage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgendaScreen(
    state: AgendaUiState,
    onBack: () -> Unit,
    onOpenObra: (String) -> Unit,
    onIniciarRelevamiento: (String) -> Unit,
    onFiltroChanged: (FiltroAgenda) -> Unit,
    onIniciarReprogramacion: (Obra) -> Unit,
    onCancelarReprogramacion: () -> Unit,
    onConfirmarReprogramacion: (Obra, Long?, Int?) -> Unit,
    onClearFeedbackMessage: () -> Unit,
    onRetry: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val feedbackTexto = state.mensajeFeedback?.let { feedbackTexto(it) }

    LaunchedEffect(feedbackTexto) {
        feedbackTexto?.let {
            snackbarHostState.showSnackbar(it)
            onClearFeedbackMessage()
        }
    }

    state.obraParaReprogramar?.let { obra ->
        DialogReprogramar(
            obra = obra,
            guardando = state.isUpdating,
            hoyMillis = state.hoyMillis,
            onCancelar = onCancelarReprogramacion,
            onConfirmar = onConfirmarReprogramacion
        )
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
                            text = stringResource(R.string.agenda_titulo),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.agenda_subtitulo),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.volver)
                        )
                    }
                }
            )
        }
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
                                    Text("${stringResource(filtro.labelRes)} ($count)")
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
                                    text = stringResource(R.string.agenda_vacio_titulo),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = stringResource(R.string.agenda_vacio_descripcion),
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
private fun feedbackTexto(feedback: FeedbackAgenda): String = when (feedback) {
    is FeedbackAgenda.VisitaProgramada -> stringResource(R.string.agenda_visita_programada, feedback.obraNombre)
    is FeedbackAgenda.VisitaCancelada -> stringResource(R.string.agenda_visita_cancelada, feedback.obraNombre)
    is FeedbackAgenda.ErrorAlProgramar -> stringResource(R.string.agenda_error_programar, feedback.error.asMessage())
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
            label = stringResource(R.string.agenda_metrica_hoy),
            valor = totalHoy.toString(),
            colorFondo = if (totalHoy > 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
            colorTexto = if (totalHoy > 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        MetricaAgendaCard(
            label = stringResource(R.string.agenda_metrica_atrasadas),
            valor = totalVencidas.toString(),
            colorFondo = if (totalVencidas > 0) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surface,
            colorTexto = if (totalVencidas > 0) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        MetricaAgendaCard(
            label = stringResource(R.string.agenda_metrica_esta_semana),
            valor = totalEstaSemana.toString(),
            colorFondo = MaterialTheme.colorScheme.surface,
            colorTexto = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        MetricaAgendaCard(
            label = stringResource(R.string.agenda_metrica_sin_fecha),
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

    // El texto del badge nunca va en secondary ni outline (poco contraste): esos acentos son solo de borde y fondo.
    val textoAcento = when (item.categoria) {
        CategoriaAgendaVisita.ESTA_SEMANA, CategoriaAgendaVisita.PROXIMAS -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> accentColor
    }

    val context = LocalContext.current
    val horaTexto = item.obra.horaVisita?.takeIf { item.fechaProgramadaMillis != null }?.let { formatHora(context, it) }
    val fechaProgramada = item.fechaProgramadaMillis?.let { formatFecha(it) }.orEmpty()
        .let { if (horaTexto != null) "$it · $horaTexto" else it }
    val badgeText = when (item.categoria) {
        CategoriaAgendaVisita.URGENTE_VENCIDA -> {
            val dias = Math.abs(item.diasDiferencia).toInt()
            // Atrasada con hora pero del mismo día: no son "0 días".
            val base = if (dias == 0) stringResource(R.string.agenda_badge_atrasada_hoy)
                else pluralStringResource(R.plurals.agenda_badge_atrasada, dias, dias)
            base + (horaTexto?.let { " · $it" } ?: "")
        }
        CategoriaAgendaVisita.HOY -> stringResource(R.string.visita_para_hoy) + (horaTexto?.let { " · $it" } ?: "")
        CategoriaAgendaVisita.ESTA_SEMANA -> {
            val dias = item.diasDiferencia.toInt()
            pluralStringResource(R.plurals.agenda_badge_esta_semana, dias, dias, fechaProgramada)
        }
        CategoriaAgendaVisita.PROXIMAS -> "⏳ $fechaProgramada"
        CategoriaAgendaVisita.SIN_PROGRAMAR -> {
            val dias = (item.diasSinVisita ?: 0L).toInt()
            pluralStringResource(R.plurals.agenda_badge_sin_fecha, dias, dias)
        }
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
                        color = textoAcento,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                TextButton(
                    onClick = onOpenObra,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = stringResource(R.string.agenda_ver_obra),
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
                        text = stringResource(R.string.agenda_cliente, item.obra.cliente),
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
                    text = item.obra.direccion.ifBlank { stringResource(R.string.sin_direccion) },
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
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = stringResource(R.string.agenda_ultimo_relevamiento, formatFecha(ultima)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        text = stringResource(R.string.agenda_relevar),
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
                        text = stringResource(R.string.agenda_mapa),
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
                        text = stringResource(if (item.fechaProgramadaMillis != null) R.string.agenda_cambiar else R.string.agenda_agendar),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
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
    } catch (e: ActivityNotFoundException) {
        Log.w("AgendaScreen", "Google Maps no está instalada; se prueba con otra app de mapas", e)
        val fallbackIntent = Intent(Intent.ACTION_VIEW, uri)
        try {
            context.startActivity(fallbackIntent)
        } catch (e: ActivityNotFoundException) {
            Log.w("AgendaScreen", "No hay ninguna app que pueda abrir el mapa", e)
        }
    }
}

/** Elegir la fecha y la hora de la próxima visita. La hora que ya tenía la obra se conserva por defecto. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogReprogramar(
    obra: Obra,
    guardando: Boolean,
    hoyMillis: Long,
    onCancelar: () -> Unit,
    onConfirmar: (Obra, Long?, Int?) -> Unit
) {
    val context = LocalContext.current
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = obra.proximaVisita ?: hoyMillis
    )
    var hora by rememberSaveable(obra.id) { mutableStateOf(obra.horaVisita) }
    var eligiendoHora by rememberSaveable { mutableStateOf(false) }
    var confirmandoQuitar by rememberSaveable { mutableStateOf(false) }

    if (eligiendoHora) {
        val inicial = hora ?: HORA_VISITA_POR_DEFECTO_MINUTOS
        val timeState = rememberTimePickerState(
            initialHour = inicial / 60,
            initialMinute = inicial % 60,
            is24Hour = android.text.format.DateFormat.is24HourFormat(context)
        )
        AlertDialog(
            onDismissRequest = { eligiendoHora = false },
            title = { Text(stringResource(R.string.agenda_hora_visita_titulo)) },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(onClick = {
                    hora = timeState.hour * 60 + timeState.minute
                    eligiendoHora = false
                }) { Text(stringResource(R.string.aceptar)) }
            },
            dismissButton = {
                TextButton(onClick = { eligiendoHora = false }) { Text(stringResource(R.string.cancelar)) }
            }
        )
    }

    if (confirmandoQuitar) {
        AlertDialog(
            onDismissRequest = { confirmandoQuitar = false },
            title = { Text(stringResource(R.string.agenda_quitar_confirmar_titulo)) },
            text = { Text(stringResource(R.string.agenda_quitar_confirmar_mensaje, obra.nombre)) },
            confirmButton = {
                TextButton(
                    enabled = !guardando,
                    onClick = {
                        confirmandoQuitar = false
                        onConfirmar(obra, null, null)
                    }
                ) { Text(stringResource(R.string.agenda_quitar_fecha), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmandoQuitar = false }) { Text(stringResource(R.string.cancelar)) }
            }
        )
    }

    DatePickerDialog(
        onDismissRequest = { if (!guardando) onCancelar() },
        confirmButton = {
            Button(
                enabled = !guardando,
                onClick = { onConfirmar(obra, pickerState.selectedDateMillis, hora) }
            ) {
                Text(stringResource(if (guardando) R.string.agenda_guardando else R.string.agenda_guardar_fecha))
            }
        },
        dismissButton = {
            Row {
                if (obra.proximaVisita != null) {
                    TextButton(enabled = !guardando, onClick = { confirmandoQuitar = true }) {
                        Text(stringResource(R.string.agenda_quitar_fecha), color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(enabled = !guardando, onClick = onCancelar) {
                    Text(stringResource(R.string.cancelar))
                }
            }
        }
    ) {
        Column {
            DatePicker(state = pickerState)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Text(
                    text = hora?.let { stringResource(R.string.agenda_hora_actual, formatHora(context, it)) }
                        ?: stringResource(R.string.agenda_sin_hora),
                    modifier = Modifier.weight(1f)
                )
                TextButton(enabled = !guardando, onClick = { eligiendoHora = true }) {
                    Text(stringResource(if (hora == null) R.string.agenda_elegir_hora else R.string.agenda_cambiar_hora))
                }
                if (hora != null) {
                    TextButton(enabled = !guardando, onClick = { hora = null }) {
                        Text(stringResource(R.string.agenda_quitar_hora))
                    }
                }
            }
        }
    }
}
