package com.unaj.appmoviles.features.obras.tareas.cronograma

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.unaj.appmoviles.features.obras.formatDiaMes
import com.unaj.appmoviles.features.obras.formatFecha
import com.unaj.appmoviles.obras.R
import com.unaj.appmoviles.obratrack.core.domain.model.EstadoTarea
import kotlin.math.roundToInt

private val ANCHO_DIA = 24.dp
// Con la escala por día una tarea de un día ya mide ANCHO_DIA; el mínimo la protege
// si en el futuro se achica la escala.
private val ANCHO_MINIMO_BARRA = 12.dp
private val ANCHO_TITULOS = 128.dp
private val ALTO_ENCABEZADO = 32.dp
// Alto para el título y las fechas de inicio y fin en dos líneas.
private val ALTO_FILA = 64.dp
private val ALTO_BARRA = 24.dp

/**
 * Cronograma tipo Gantt: títulos fijos a la izquierda y barras con scroll horizontal.
 * Solo dibuja; las fechas y estados llegan ya calculados en [cronograma].
 * No tiene scroll vertical: títulos y barras van en el mismo Row, así que el que lo contiene
 * puede darle scroll vertical sin que se desalineen.
 */
@Composable
fun GanttChart(
    cronograma: Cronograma,
    onTareaClick: (tareaId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var anchoVisible by remember { mutableIntStateOf(0) }
    // Se centra en hoy una sola vez; después se respeta el scroll del usuario (también al rotar).
    var centrado by rememberSaveable { mutableStateOf(false) }
    val anchoDiaPx = with(LocalDensity.current) { ANCHO_DIA.toPx() }
    val periodo = cronograma.periodo

    LaunchedEffect(anchoVisible, cronograma.hoyDia, periodo) {
        val hoyDia = cronograma.hoyDia
        if (centrado || anchoVisible == 0 || hoyDia == null || periodo == null) return@LaunchedEffect
        val destino = desplazamientoInicial(
            hoyDia = hoyDia,
            anchoDia = anchoDiaPx,
            anchoVisible = anchoVisible.toFloat(),
            anchoTotal = periodo.dias * anchoDiaPx
        )
        scrollState.scrollTo(destino.roundToInt())
        centrado = true
    }

    if (periodo == null) return

    Row(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.width(ANCHO_TITULOS)) {
            Box(modifier = Modifier.height(ALTO_ENCABEZADO))
            cronograma.barras.forEach { barra ->
                TituloFila(barra = barra, onClick = { onTareaClick(barra.tarea.id) })
            }
        }

        val colorHoy = MaterialTheme.colorScheme.error
        val colorSemana = MaterialTheme.colorScheme.outlineVariant
        val semanas = periodo.iniciosDeSemana()

        Column(
            modifier = Modifier
                .weight(1f)
                .onSizeChanged { anchoVisible = it.width }
                .horizontalScroll(scrollState)
                .width(ANCHO_DIA * periodo.dias)
                .drawWithContent {
                    val anchoDia = ANCHO_DIA.toPx()
                    semanas.forEach { dia ->
                        val x = dia * anchoDia
                        drawLine(colorSemana, Offset(x, 0f), Offset(x, size.height), 1.dp.toPx())
                    }
                    drawContent()
                    cronograma.hoyDia?.let { dia ->
                        // La línea va en el centro del día de hoy.
                        val x = (dia + 0.5f) * anchoDia
                        drawLine(colorHoy, Offset(x, 0f), Offset(x, size.height), 2.dp.toPx())
                    }
                }
        ) {
            EncabezadoSemanas(periodo = periodo, semanas = semanas, hoyDia = cronograma.hoyDia)
            cronograma.barras.forEach { barra ->
                FilaBarra(barra = barra, onClick = { onTareaClick(barra.tarea.id) })
            }
        }
    }
}

@Composable
private fun TituloFila(barra: BarraGantt, onClick: () -> Unit) {
    val tarea = barra.tarea
    Column(
        modifier = Modifier
            .height(ALTO_FILA)
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (barra.atrasada) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = stringResource(R.string.tarea_atrasada),
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .size(16.dp)
                        .padding(end = 4.dp)
                )
            }
            Text(
                text = tarea.titulo,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        val inicio = tarea.fechaInicio
        val fin = tarea.fechaFin
        if (inicio != null && fin != null) {
            // Inicio y fin en líneas separadas: en una sola no entran en el ancho de la columna.
            Text(
                text = stringResource(R.string.cronograma_fecha_inicio, formatFecha(inicio)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            Text(
                text = formatFecha(fin),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
    HorizontalDivider()
}

@Composable
private fun EncabezadoSemanas(periodo: PeriodoVisible, semanas: List<Int>, hoyDia: Int?) {
    Box(modifier = Modifier.height(ALTO_ENCABEZADO)) {
        semanas.forEach { dia ->
            Text(
                text = periodo.inicio.plusDays(dia.toLong()).let(::formatDiaMes),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .offset(x = ANCHO_DIA * dia)
                    .padding(start = 4.dp, top = 2.dp)
            )
        }
        if (hoyDia != null) {
            Text(
                text = stringResource(R.string.cronograma_hoy),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = ANCHO_DIA * hoyDia)
            )
        }
    }
}

@Composable
private fun FilaBarra(barra: BarraGantt, onClick: () -> Unit) {
    val (relleno, borde) = coloresBarra(barra)
    val ancho: Dp = anchoBarra(barra.duracionDias, ANCHO_DIA.value, ANCHO_MINIMO_BARRA.value).dp
    val forma = RoundedCornerShape(6.dp)

    Box(modifier = Modifier.height(ALTO_FILA), contentAlignment = Alignment.CenterStart) {
        Box(
            modifier = Modifier
                .offset(x = ANCHO_DIA * barra.desdeDia)
                .width(ancho)
                .height(ALTO_BARRA)
                .padding(horizontal = 1.dp)
                .background(relleno, forma)
                .border(BorderStroke(grosorBorde(barra), borde), forma)
                .clickable(onClick = onClick)
        )
    }
    HorizontalDivider()
}

/** En curso y atrasada llevan borde grueso: así no dependen solo del relleno, que en claro casi no se ve. */
private fun grosorBorde(barra: BarraGantt): Dp =
    if (barra.atrasada || barra.tarea.estado == EstadoTarea.EN_CURSO) 2.dp else 1.dp

/**
 * El estado se distingue por cuánto se llena la barra: pendiente vacía, en curso clara,
 * completada sólida. El rojo (borde de error) se reserva para las atrasadas.
 */
@Composable
private fun coloresBarra(barra: BarraGantt): Pair<Color, Color> {
    val colors = MaterialTheme.colorScheme
    val (relleno, bordeEstado) = when (barra.tarea.estado) {
        EstadoTarea.PENDIENTE -> colors.surface to colors.outline
        EstadoTarea.EN_CURSO -> colors.secondaryContainer to colors.primary
        EstadoTarea.COMPLETADA -> colors.tertiary to colors.tertiary
    }
    val borde = if (barra.atrasada) colors.error else bordeEstado
    return relleno to borde
}
