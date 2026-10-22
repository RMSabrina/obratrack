package com.unaj.appmoviles.obratrack.core.domain.usecase.tareas

import com.unaj.appmoviles.obratrack.core.domain.model.EstadoTarea
import com.unaj.appmoviles.obratrack.core.domain.model.ResumenTareas
import com.unaj.appmoviles.obratrack.core.domain.model.Tarea
import java.time.LocalDate

/** Planificada: tiene ambas fechas. Solo estas tareas se dibujan en el cronograma. */
fun Tarea.estaPlanificada(): Boolean = fechaInicio != null && fechaFin != null

/**
 * Atrasada: tiene fecha de fin, esa fecha ya pasó y no está completada.
 * Si la fecha de fin es hoy, todavía no está atrasada. No modifica el estado almacenado.
 */
fun Tarea.estaAtrasada(hoy: LocalDate): Boolean {
    val fin = fechaFin ?: return false
    return estado != EstadoTarea.COMPLETADA && fin.isBefore(hoy)
}

/** Orden por fecha de inicio ascendente, sin fecha al final; desempate por título. */
fun List<Tarea>.ordenadas(): List<Tarea> = sortedWith(
    compareBy<Tarea, LocalDate?>(nullsLast()) { it.fechaInicio }
        .thenBy(String.CASE_INSENSITIVE_ORDER) { it.titulo }
)

/** Resumen para el detalle de obra: las atrasadas también cuentan como pendientes. */
fun List<Tarea>.resumen(hoy: LocalDate): ResumenTareas {
    val completadas = count { it.estado == EstadoTarea.COMPLETADA }
    return ResumenTareas(
        completadas = completadas,
        pendientes = size - completadas,
        atrasadas = count { it.estaAtrasada(hoy) }
    )
}
