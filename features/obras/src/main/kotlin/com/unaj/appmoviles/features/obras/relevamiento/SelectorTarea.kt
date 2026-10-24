package com.unaj.appmoviles.features.obras.relevamiento

import com.unaj.appmoviles.obratrack.core.domain.model.EstadoTarea
import com.unaj.appmoviles.obratrack.core.domain.model.Tarea
import com.unaj.appmoviles.obratrack.core.domain.usecase.tareas.ordenadas

/** Una tarea elegible en el selector del formulario de relevamiento. */
data class OpcionTarea(val id: String, val titulo: String, val completada: Boolean)

data class SelectorTareaUiState(
    /** Solo el dueño, con las tareas ya leídas, ve el selector. */
    val disponible: Boolean = false,
    val opciones: List<OpcionTarea> = emptyList(),
    /** null = "Ninguna". */
    val seleccionada: String? = null,
    /** No se pudieron leer las tareas: se conserva el vínculo que tenía el relevamiento. */
    val error: Boolean = false
)

/** Todas las tareas, en el mismo orden que el listado de tareas. */
fun List<Tarea>.opcionesSelector(): List<OpcionTarea> =
    ordenadas().map { OpcionTarea(it.id, it.titulo, it.estado == EstadoTarea.COMPLETADA) }

/**
 * Vínculo a mostrar: si la tarea ya no existe (borrada desde otro dispositivo, por ejemplo),
 * el relevamiento se ve sin tarea y al guardar se borra el campo.
 */
fun tareaVinculadaVigente(tareaId: String?, tareas: List<Tarea>): String? =
    tareaId?.takeIf { id -> tareas.any { it.id == id } }
