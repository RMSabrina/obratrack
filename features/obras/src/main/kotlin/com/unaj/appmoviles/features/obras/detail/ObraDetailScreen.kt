package com.unaj.appmoviles.features.obras.detail

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.rememberCoroutineScope
import com.unaj.appmoviles.features.obras.export.RelevamientoPdfExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.unaj.appmoviles.features.obras.formatFecha
import com.unaj.appmoviles.features.obras.label
import com.unaj.appmoviles.features.obras.tareas.cronograma.CronogramaRoute
import com.unaj.appmoviles.features.obras.tareas.list.TareasListRoute
import com.unaj.appmoviles.obras.R
import com.unaj.appmoviles.obratrack.core.domain.model.EstadoObra
import com.unaj.appmoviles.obratrack.core.domain.model.Obra
import com.unaj.appmoviles.obratrack.core.domain.model.Relevamiento
import com.unaj.appmoviles.obratrack.core.domain.model.ResumenTareas
import com.unaj.appmoviles.obratrack.core.domain.ui.LoadingContent
import com.unaj.appmoviles.obratrack.core.domain.ui.asMessage
import com.unaj.appmoviles.obratrack.core.ui.components.EmptyState
import com.unaj.appmoviles.obratrack.core.ui.components.InfoRow
import com.unaj.appmoviles.obratrack.core.ui.components.StatusBadge

@Composable
fun ObraDetailRoute(
    obraId: String,
    onBack: () -> Unit,
    onDeleteSuccess: () -> Unit,
    onEditObra: (Obra) -> Unit,
    onCreateRelevamiento: () -> Unit,
    onOpenRelevamiento: (Relevamiento) -> Unit,
    onCreateTarea: () -> Unit,
    onOpenTarea: (tareaId: String) -> Unit,
    viewModel: ObraDetailViewModel = hiltViewModel(
        creationCallback = { factory: ObraDetailViewModel.Factory -> factory.create(obraId) }
    )
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val shareTitle = stringResource(R.string.compartir_informe_obra_via)

    ObraDetailScreen(
        state = state,
        onBack = onBack,
        onDeleteSuccess = onDeleteSuccess,
        onDeleteObra = viewModel::deleteObra,
        onDeleteErrorDismissed = viewModel::onDeleteErrorDismissed,
        onEditObra = onEditObra,
        onCreateRelevamiento = onCreateRelevamiento,
        onOpenRelevamiento = onOpenRelevamiento,
        onCreateTarea = onCreateTarea,
        onShareInforme = {
            val obra = state.obra
            if (obra != null && !state.isDeleting) {
                coroutineScope.launch(Dispatchers.IO) {
                    val uri = RelevamientoPdfExporter.exportInformeObraToPdf(
                        context = context,
                        obra = obra,
                        resumenTareas = state.resumenTareas,
                        relevamientos = state.relevamientos,
                        tareas = state.tareas
                    )
                    if (uri != null) {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/pdf"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        val chooser = Intent.createChooser(intent, shareTitle)
                        context.startActivity(chooser)
                    }
                }
            }
        },
        // Las pestañas Tareas y Cronograma tienen su propio ViewModel, que vuelve a verificar
        // el dueño. Solo se crean al abrir la pestaña.
        tareasTab = { modifier ->
            TareasListRoute(
                obraId = obraId,
                onCreateTarea = onCreateTarea,
                onOpenTarea = { tarea -> onOpenTarea(tarea.id) },
                modifier = modifier
            )
        },
        cronogramaTab = { onOpenTareas, modifier ->
            CronogramaRoute(
                obraId = obraId,
                onOpenTarea = onOpenTarea,
                onOpenTareas = onOpenTareas,
                modifier = modifier
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ObraDetailScreen(
    state: ObraDetailUiState,
    onBack: () -> Unit,
    onDeleteSuccess: () -> Unit,
    onDeleteObra: () -> Unit,
    onDeleteErrorDismissed: () -> Unit,
    onEditObra: (Obra) -> Unit,
    onCreateRelevamiento: () -> Unit,
    onOpenRelevamiento: (Relevamiento) -> Unit,
    onCreateTarea: () -> Unit,
    onShareInforme: () -> Unit = {},
    tareasTab: @Composable (Modifier) -> Unit,
    cronogramaTab: @Composable (onOpenTareas: () -> Unit, Modifier) -> Unit,
    modifier: Modifier = Modifier
) {
    val obra = state.obra
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    // Se conserva al volver del detalle o del formulario de una tarea (decorador de Nav3).
    var pestana by rememberSaveable { mutableStateOf(ObraDetailTab.RESUMEN) }
    val pestanas = pestanasVisibles(esDueno = state.isEditable)
    val pestanaActual = pestana.takeIf { it in pestanas } ?: ObraDetailTab.RESUMEN
    val snackbarHostState = remember { SnackbarHostState() }

    BackHandler(enabled = state.isDeleting) {}

    LaunchedEffect(state.isDeleted) {
        if (state.isDeleted) {
            onDeleteSuccess()
        }
    }

    val deleteErrorMessage = state.deleteError?.asMessage()
    LaunchedEffect(deleteErrorMessage) {
        if (deleteErrorMessage != null) {
            snackbarHostState.showSnackbar(deleteErrorMessage)
            onDeleteErrorDismissed()
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { if (!state.isDeleting) showDeleteDialog = false },
            title = { Text("Eliminar Obra") },
            text = { Text("¿Eliminar Obra y todos sus relevamientos, tareas y suscriptores? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(
                    enabled = !state.isDeleting,
                    onClick = {
                        showDeleteDialog = false
                        onDeleteObra()
                    }
                ) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !state.isDeleting,
                    onClick = { showDeleteDialog = false }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Detalle de obra") },
                navigationIcon = {
                    IconButton(
                        onClick = { if (!state.isDeleting) onBack() },
                        enabled = !state.isDeleting
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                actions = {
                    if (obra != null) {
                        IconButton(
                            onClick = onShareInforme,
                            enabled = !state.isDeleting
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = stringResource(R.string.compartir_informe_obra)
                            )
                        }
                        if (state.isEditable) {
                            IconButton(
                                onClick = { if (!state.isDeleting) showDeleteDialog = true },
                                enabled = !state.isDeleting
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Eliminar obra",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                            IconButton(
                                onClick = { if (!state.isDeleting) onEditObra(obra) },
                                enabled = !state.isDeleting
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Editar obra"
                                )
                            }
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            val fab = pestanaActual.fab()
            if (obra != null && state.isEditable && !state.isDeleting && fab != null) {
                val (texto, onClick) = when (fab) {
                    FabDetalle.NUEVO_RELEVAMIENTO -> stringResource(R.string.obra_relevamiento_nuevo) to onCreateRelevamiento
                    FabDetalle.NUEVA_TAREA -> stringResource(R.string.tarea_nueva) to onCreateTarea
                }
                ExtendedFloatingActionButton(
                    onClick = onClick,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text(texto) }
                )
            }
        }
    ) { padding ->
        when {
            state.isLoading -> LoadingContent(Modifier.padding(padding))
            obra == null -> {
                Text(
                    text = state.error?.asMessage() ?: "No encontramos esa obra",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .padding(padding)
                        .padding(24.dp)
                )
            }
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                ObraDetailTabRow(
                    pestanas = pestanas,
                    seleccionada = pestanaActual,
                    atrasadas = insigniaAtrasadas(state.resumenTareas),
                    onSelect = { pestana = it }
                )
                val contenido = Modifier.weight(1f)
                when (pestanaActual) {
                    ObraDetailTab.RESUMEN -> ResumenTab(
                        state = state,
                        obra = obra,
                        onOpenRelevamiento = onOpenRelevamiento,
                        onVerTodos = { pestana = ObraDetailTab.RELEVAMIENTOS },
                        modifier = contenido
                    )
                    ObraDetailTab.TAREAS -> tareasTab(contenido)
                    ObraDetailTab.CRONOGRAMA -> cronogramaTab({ pestana = ObraDetailTab.TAREAS }, contenido)
                    ObraDetailTab.RELEVAMIENTOS -> RelevamientosTab(
                        state = state,
                        onCreateRelevamiento = onCreateRelevamiento,
                        onOpenRelevamiento = onOpenRelevamiento,
                        modifier = contenido
                    )
                }
            }
        }
    }
}

/**
 * Sin swipe: el contenido cambia solo al tocar una pestaña. Con cuatro pestañas los títulos no
 * entran en partes iguales en un celular: cada pestaña toma el ancho de su título y la fila se
 * desliza (nunca se cortan los textos). Sin margen inicial, la pestaña siguiente se asoma en el
 * borde, y la fila centra sola la pestaña seleccionada cuando cambia.
 */
@Composable
private fun ObraDetailTabRow(
    pestanas: List<ObraDetailTab>,
    seleccionada: ObraDetailTab,
    atrasadas: Int?,
    onSelect: (ObraDetailTab) -> Unit
) {
    PrimaryScrollableTabRow(
        selectedTabIndex = pestanas.indexOf(seleccionada),
        edgePadding = 0.dp
    ) {
        pestanas.forEach { tab ->
            Tab(
                selected = tab == seleccionada,
                onClick = { onSelect(tab) },
                text = {
                    PestanaTitulo(
                        tab = tab,
                        atrasadas = if (tab == ObraDetailTab.TAREAS) atrasadas else null
                    )
                }
            )
        }
    }
}

// Para revisar en Android Studio que los títulos no se corten y que la pestaña siguiente se
// asome: celular angosto (360 dp) y letra del sistema agrandada.
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, fontScale = 1.3f)
@Composable
private fun ObraDetailTabRowPreview() {
    ObraDetailTabRow(
        pestanas = pestanasVisibles(esDueno = true),
        seleccionada = ObraDetailTab.RESUMEN,
        atrasadas = 2,
        onSelect = {}
    )
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun ObraDetailTabRowSeleccionadaPreview() {
    ObraDetailTabRow(
        pestanas = pestanasVisibles(esDueno = true),
        seleccionada = ObraDetailTab.RELEVAMIENTOS,
        atrasadas = null,
        onSelect = {}
    )
}

/** Título de la pestaña; en Tareas lleva la insignia con las atrasadas, si hay. */
@Composable
private fun PestanaTitulo(tab: ObraDetailTab, atrasadas: Int?) {
    val titulo = stringResource(tab.labelRes())
    if (atrasadas == null) {
        Text(titulo, maxLines = 1)
        return
    }
    val descripcion = pluralStringResource(R.plurals.obra_tareas_atrasadas_insignia, atrasadas, atrasadas)
    // La insignia va al lado del texto, no encima: así nunca tapa el título.
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        // TalkBack lee "Tareas, 1 tarea atrasada" en vez de "Tareas, 1".
        modifier = Modifier.clearAndSetSemantics { contentDescription = "$titulo, $descripcion" }
    ) {
        Text(titulo, maxLines = 1)
        Badge(containerColor = MaterialTheme.colorScheme.error) { Text(atrasadas.toString()) }
    }
}

/** Datos de la obra, resumen de tareas (solo dueño) y los últimos relevamientos. */
@Composable
private fun ResumenTab(
    state: ObraDetailUiState,
    obra: Obra,
    onOpenRelevamiento: (Relevamiento) -> Unit,
    onVerTodos: () -> Unit,
    modifier: Modifier = Modifier
) {
    val recientes = state.relevamientos.recientes()
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ObraHeaderCard(obra = obra)
        }

        // Las tareas son privadas del dueño. Sin resumen (error al leerlas) no se muestra la sección.
        val resumen = state.resumenTareas
        if (state.isEditable && resumen != null) {
            item {
                SectionTitle(stringResource(R.string.obras_tareas))
            }
            item {
                TareasResumenCard(resumen = resumen)
            }
        }

        item {
            SectionTitle(stringResource(R.string.obra_seccion_relevamientos))
        }
        if (state.relevamientosError) {
            item { RelevamientosErrorMensaje() }
        } else if (recientes.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.obra_relevamientos_vacio),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(recientes, key = { it.id }) { relevamiento ->
                RelevamientoCard(
                    relevamiento = relevamiento,
                    isEditable = state.isEditable && !state.isDeleting,
                    onClick = { if (!state.isDeleting) onOpenRelevamiento(relevamiento) }
                )
            }
        }
        // Solo si hay más de los que entran en el resumen.
        if (state.relevamientos.size > recientes.size) {
            item {
                TextButton(onClick = onVerTodos, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.obra_relevamientos_ver_todos, state.relevamientos.size))
                }
            }
        }
    }
}

/** Aviso de la sección cuando fallan los relevamientos (también en la obra pública). */
@Composable
internal fun RelevamientosErrorMensaje(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.obra_relevamientos_error),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error,
        modifier = modifier
    )
}

/** Historial completo de relevamientos, del más nuevo al más viejo. */
@Composable
private fun RelevamientosTab(
    state: ObraDetailUiState,
    onCreateRelevamiento: () -> Unit,
    onOpenRelevamiento: (Relevamiento) -> Unit,
    modifier: Modifier = Modifier
) {
    val puedeCrear = state.isEditable && !state.isDeleting
    if (state.relevamientosError) {
        RelevamientosErrorMensaje(modifier.padding(24.dp))
        return
    }
    if (state.relevamientos.isEmpty()) {
        EmptyState(
            icon = Icons.Default.Info,
            title = stringResource(R.string.obra_relevamientos_vacio_titulo),
            description = stringResource(R.string.obra_relevamientos_vacio_descripcion),
            actionLabel = if (puedeCrear) stringResource(R.string.obra_relevamientos_agregar_primero) else null,
            onActionClick = if (puedeCrear) onCreateRelevamiento else null,
            modifier = modifier
        )
        return
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(state.relevamientos, key = { it.id }) { relevamiento ->
            RelevamientoCard(
                relevamiento = relevamiento,
                isEditable = puedeCrear,
                onClick = { if (!state.isDeleting) onOpenRelevamiento(relevamiento) }
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

/** Contadores de tareas; para verlas o planificarlas están las pestañas Tareas y Cronograma. */
@Composable
private fun TareasResumenCard(
    resumen: ResumenTareas,
    modifier: Modifier = Modifier
) {
    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when {
                resumen.completadas + resumen.pendientes == 0 -> Text(
                    text = stringResource(R.string.obra_tareas_sin_tareas),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                else -> Row(modifier = Modifier.fillMaxWidth()) {
                    ContadorTareas(
                        valor = resumen.completadas,
                        etiqueta = stringResource(R.string.obra_tareas_completadas),
                        modifier = Modifier.weight(1f)
                    )
                    ContadorTareas(
                        valor = resumen.pendientes,
                        etiqueta = stringResource(R.string.obra_tareas_pendientes),
                        modifier = Modifier.weight(1f)
                    )
                    ContadorTareas(
                        valor = resumen.atrasadas,
                        etiqueta = stringResource(R.string.obra_tareas_atrasadas),
                        destacado = resumen.atrasadas > 0,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ContadorTareas(
    valor: Int,
    etiqueta: String,
    modifier: Modifier = Modifier,
    destacado: Boolean = false
) {
    val color = if (destacado) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = valor.toString(),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = etiqueta,
            style = MaterialTheme.typography.bodySmall,
            color = if (destacado) color else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ObraHeaderCard(obra: Obra, modifier: Modifier = Modifier) {
    val statusColor = when (obra.estado) {
        EstadoObra.ACTIVA -> MaterialTheme.colorScheme.primaryContainer
        EstadoObra.PAUSADA -> MaterialTheme.colorScheme.surfaceVariant
        EstadoObra.FINALIZADA -> MaterialTheme.colorScheme.tertiaryContainer
    }
    val statusTextColor = when (obra.estado) {
        EstadoObra.ACTIVA -> MaterialTheme.colorScheme.onPrimaryContainer
        EstadoObra.PAUSADA -> MaterialTheme.colorScheme.onSurfaceVariant
        EstadoObra.FINALIZADA -> MaterialTheme.colorScheme.onTertiaryContainer
    }

    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = obra.nombre,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                StatusBadge(
                    text = obra.estado.label(),
                    backgroundColor = statusColor,
                    textColor = statusTextColor,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            InfoRow(
                icon = Icons.Default.Person,
                text = obra.cliente,
                contentDescription = "Cliente"
            )
            InfoRow(
                icon = Icons.Default.Place,
                text = obra.direccion,
                contentDescription = "Dirección"
            )

            val fechas = buildString {
                append("Inicio: ${formatFecha(obra.fechaInicio)}")
                val visita = obra.ultimaVisita
                if (visita != null) {
                    append(" · Última visita: ${formatFecha(visita)}")
                }
            }
            InfoRow(
                icon = Icons.Default.DateRange,
                text = fechas,
                contentDescription = "Fechas"
            )

            obra.proximaVisita?.let { proxima ->
                val isPast = proxima < System.currentTimeMillis()
                val proximaTexto = if (isPast) {
                    "Próxima visita: " + formatFecha(proxima) + " (Atrasada)"
                } else {
                    "Próxima visita: " + formatFecha(proxima)
                }
                InfoRow(
                    icon = if (isPast) Icons.Default.Warning else Icons.Default.Alarm,
                    text = proximaTexto,
                    contentDescription = "Próxima visita"
                )
            }
        }
    }
}

@Composable
private fun RelevamientoCard(
    relevamiento: Relevamiento,
    isEditable: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    val cardModifier = if (isEditable) {
        modifier.fillMaxWidth().clickable(onClick = onClick)
    } else {
        modifier.fillMaxWidth()
    }

    ElevatedCard(modifier = cardModifier) {
        Column {
            relevamiento.fotoUrl?.let { url ->
                AsyncImage(
                    model = url,
                    contentDescription = "Foto del relevamiento",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                )
            }
            
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = formatFecha(relevamiento.fecha),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (relevamiento.nota.isNotBlank()) {
                    Text(
                        text = relevamiento.nota,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                
                relevamiento.ubicacion?.let { ubicacion ->
                    Spacer(modifier = Modifier.height(4.dp))
                    TextButton(
                        onClick = {
                            val geo = Uri.parse(
                                "geo:${ubicacion.latitud},${ubicacion.longitud}?q=${ubicacion.latitud},${ubicacion.longitud}"
                            )
                            context.startActivity(Intent(Intent.ACTION_VIEW, geo))
                        },
                        contentPadding = PaddingValues(horizontal = 0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Text("Ver en el mapa")
                    }
                }
            }
        }
    }
}
