package com.unaj.appmoviles.features.obras.tareas.list

import com.unaj.appmoviles.obratrack.core.domain.error.DomainError
import com.unaj.appmoviles.obratrack.core.domain.model.Tarea

/** Tarea lista para mostrar: [atrasada] se calcula en el ViewModel con el Clock. */
data class TareaItem(
    val tarea: Tarea,
    val atrasada: Boolean
)

data class TareasListUiState(
    val isLoading: Boolean = true,
    val tareas: List<TareaItem> = emptyList(),
    val error: DomainError? = null,
    /** Ids de tareas con cambios guardados en el teléfono que el servidor aún no confirmó. */
    val pendientes: Set<String> = emptySet()
)
