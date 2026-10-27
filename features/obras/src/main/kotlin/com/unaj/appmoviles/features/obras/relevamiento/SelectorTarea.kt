package com.unaj.appmoviles.features.obras.relevamiento

import com.unaj.appmoviles.obratrack.core.domain.model.EstadoTarea
import com.unaj.appmoviles.obratrack.core.domain.model.Tarea
import com.unaj.appmoviles.obratrack.core.domain.usecase.tareas.ordenadas
import java.time.LocalDate

/**
 * Una tarea elegible en el selector del formulario de relevamiento. Si hay otra tarea con el mismo
 * título, [repetida] es true y se completan [numero] (1, 2, ...) y las fechas para distinguirlas.
 */
data class OpcionTarea(
    val id: String,
    val titulo: String,
    val completada: Boolean,
    val repetida: Boolean = false,
    val numero: Int = 0,
    val fechaInicio: LocalDate? = null,
    val fechaFin: LocalDate? = null
)

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
fun List<Tarea>.opcionesSelector(): List<OpcionTarea> {
    val ordenadas = ordenadas()
    fun clave(tarea: Tarea) = tarea.titulo.trim().lowercase()
    val repeticiones = ordenadas.groupingBy(::clave).eachCount()
    val vistas = mutableMapOf<String, Int>()
    return ordenadas.map { tarea ->
        val completada = tarea.estado == EstadoTarea.COMPLETADA
        if ((repeticiones[clave(tarea)] ?: 0) > 1) {
            val numero = vistas.merge(clave(tarea), 1, Int::plus) ?: 1
            OpcionTarea(tarea.id, tarea.titulo, completada, true, numero, tarea.fechaInicio, tarea.fechaFin)
        } else {
            OpcionTarea(tarea.id, tarea.titulo, completada)
        }
    }
}

/**
 * Vínculo a mostrar: si la tarea ya no existe (borrada desde otro dispositivo, por ejemplo),
 * el relevamiento se ve sin tarea y al guardar se borra el campo.
 */
fun tareaVinculadaVigente(tareaId: String?, tareas: List<Tarea>): String? =
    tareaId?.takeIf { id -> tareas.any { it.id == id } }
