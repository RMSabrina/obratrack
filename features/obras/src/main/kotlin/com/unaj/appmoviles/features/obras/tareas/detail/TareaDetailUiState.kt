package com.unaj.appmoviles.features.obras.tareas.detail

import com.unaj.appmoviles.obratrack.core.domain.error.DomainError
import com.unaj.appmoviles.obratrack.core.domain.model.Relevamiento
import com.unaj.appmoviles.obratrack.core.domain.model.Tarea

data class TareaDetailUiState(
    val isLoading: Boolean = true,
    val tarea: Tarea? = null,
    /** Calculado con el Clock; nunca cambia el estado almacenado. */
    val atrasada: Boolean = false,
    val error: DomainError? = null,
    /** Relevamientos vinculados a la tarea (solo se consultan si es dueño). */
    val vinculados: List<Relevamiento> = emptyList(),
    /** No se pudieron leer los vinculados: la lista avisa del error y no se ofrece el borrado. */
    val vinculadosError: Boolean = false,
    val isCompleting: Boolean = false,
    /** Error al marcar como completada (se muestra en un Snackbar). */
    val actionError: DomainError? = null,
    val isDeleting: Boolean = false,
    val isDeleted: Boolean = false,
    val deleteError: DomainError? = null
)
