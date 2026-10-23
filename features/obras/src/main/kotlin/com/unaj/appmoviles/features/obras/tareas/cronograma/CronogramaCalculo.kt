package com.unaj.appmoviles.features.obras.tareas.cronograma

import com.unaj.appmoviles.obratrack.core.domain.model.Tarea
import com.unaj.appmoviles.obratrack.core.domain.usecase.tareas.estaAtrasada
import com.unaj.appmoviles.obratrack.core.domain.usecase.tareas.estaPlanificada
import com.unaj.appmoviles.obratrack.core.domain.usecase.tareas.ordenadas
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Días de margen que se agregan antes y después del período visible. */
const val MARGEN_DIAS = 3

/** Rango de días que muestra el cronograma, con ambos extremos incluidos. */
data class PeriodoVisible(val inicio: LocalDate, val fin: LocalDate) {
    val dias: Int get() = diasEntre(inicio, fin) + 1

    /** Índice del día dentro del período (0 = inicio), o null si la fecha queda afuera. */
    fun indiceDe(fecha: LocalDate): Int? =
        if (fecha.isBefore(inicio) || fecha.isAfter(fin)) null else diasEntre(inicio, fecha)
}

/** Barra de una tarea: el día del período en que empieza y cuántos días ocupa. */
data class BarraGantt(
    val tarea: Tarea,
    val desdeDia: Int,
    val duracionDias: Int,
    val atrasada: Boolean
)

/**
 * Datos ya calculados para dibujar el Gantt. Sin tareas planificadas, [periodo] es null
 * y no hay barras. [sinFechas] cuenta las tareas que no se dibujan.
 */
data class Cronograma(
    val periodo: PeriodoVisible?,
    val barras: List<BarraGantt>,
    val hoyDia: Int?,
    val sinFechas: Int
)

/**
 * Período visible: desde el inicio más temprano hasta el fin más tardío de las tareas
 * planificadas, ampliado para incluir [hoy] y con [margenDias] a cada lado.
 * Devuelve null si ninguna tarea tiene ambas fechas.
 */
fun calcularPeriodo(
    tareas: List<Tarea>,
    hoy: LocalDate,
    margenDias: Int = MARGEN_DIAS
): PeriodoVisible? {
    val planificadas = tareas.filter { it.estaPlanificada() }
    if (planificadas.isEmpty()) return null
    val primerInicio = minOf(planificadas.mapNotNull { it.fechaInicio }.min(), hoy)
    val ultimoFin = maxOf(planificadas.mapNotNull { it.fechaFin }.max(), hoy)
    return PeriodoVisible(
        inicio = primerInicio.minusDays(margenDias.toLong()),
        fin = ultimoFin.plusDays(margenDias.toLong())
    )
}

/** Barra de la tarea dentro del período; null si no tiene ambas fechas. El fin se incluye. */
fun Tarea.toBarra(periodo: PeriodoVisible, hoy: LocalDate): BarraGantt? {
    val inicio = fechaInicio ?: return null
    val fin = fechaFin ?: return null
    return BarraGantt(
        tarea = this,
        desdeDia = diasEntre(periodo.inicio, inicio),
        duracionDias = diasEntre(inicio, fin) + 1,
        atrasada = estaAtrasada(hoy)
    )
}

/** Ancho de una barra en la misma unidad que [anchoDia], nunca menor que [anchoMinimo]. */
fun anchoBarra(duracionDias: Int, anchoDia: Float, anchoMinimo: Float): Float =
    maxOf(duracionDias * anchoDia, anchoMinimo)

/** Índices de los lunes dentro del período, para el encabezado de semanas. */
fun PeriodoVisible.iniciosDeSemana(): List<Int> =
    (0 until dias).filter { inicio.plusDays(it.toLong()).dayOfWeek == DayOfWeek.MONDAY }

/**
 * Scroll horizontal inicial para que el centro del día [hoyDia] quede en el centro de la
 * parte visible, sin pasarse de los bordes. Todas las medidas en la misma unidad (px).
 */
fun desplazamientoInicial(
    hoyDia: Int,
    anchoDia: Float,
    anchoVisible: Float,
    anchoTotal: Float
): Float {
    val centroHoy = (hoyDia + 0.5f) * anchoDia
    val maximo = maxOf(anchoTotal - anchoVisible, 0f)
    return (centroHoy - anchoVisible / 2).coerceIn(0f, maximo)
}

/** Arma el cronograma con las tareas ordenadas; solo las planificadas tienen barra. */
fun calcularCronograma(tareas: List<Tarea>, hoy: LocalDate): Cronograma {
    val sinFechas = tareas.count { !it.estaPlanificada() }
    val periodo = calcularPeriodo(tareas, hoy)
        ?: return Cronograma(periodo = null, barras = emptyList(), hoyDia = null, sinFechas = sinFechas)
    return Cronograma(
        periodo = periodo,
        barras = tareas.ordenadas().mapNotNull { it.toBarra(periodo, hoy) },
        hoyDia = periodo.indiceDe(hoy),
        sinFechas = sinFechas
    )
}

private fun diasEntre(desde: LocalDate, hasta: LocalDate): Int =
    ChronoUnit.DAYS.between(desde, hasta).toInt()
