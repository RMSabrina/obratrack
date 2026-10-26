package com.unaj.appmoviles.features.obras.export

import com.unaj.appmoviles.obratrack.core.domain.model.Tarea

/**
 * Título de la tarea vinculada de un relevamiento, para la línea "Tarea Vinculada" del PDF.
 * Sin tarea, o con una tarea que ya no existe, devuelve null y la línea se omite (no se
 * inventa una tarea ni se muestra el id).
 */
internal fun tareaVinculadaTitulo(tareaId: String?, tareas: List<Tarea>): String? =
    tareaId
        ?.takeIf { it.isNotBlank() }
        ?.let { id -> tareas.find { it.id == id } }
        ?.titulo
