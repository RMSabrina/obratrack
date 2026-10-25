package com.unaj.appmoviles.features.obras.export

import com.unaj.appmoviles.obratrack.core.domain.model.Tarea

/**
 * Línea "Tarea Vinculada" de un relevamiento en el PDF. Sin tarea, o con una tarea que ya
 * no existe, devuelve null y la línea se omite (no se inventa una tarea ni se muestra el id).
 */
internal fun tareaVinculadaTexto(tareaId: String?, tareas: List<Tarea>): String? =
    tareaId
        ?.takeIf { it.isNotBlank() }
        ?.let { id -> tareas.find { it.id == id } }
        ?.let { "Tarea Vinculada: ${it.titulo}" }
