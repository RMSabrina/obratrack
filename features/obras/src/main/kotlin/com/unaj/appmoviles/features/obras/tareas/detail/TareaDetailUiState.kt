package com.unaj.appmoviles.features.obras.tareas.detail

import com.unaj.appmoviles.obratrack.core.domain.error.DomainError
import com.unaj.appmoviles.obratrack.core.domain.model.Tarea

data class TareaDetailUiState(
    val isLoading: Boolean = true,
    val tarea: Tarea? = null,
    /** Calculado con el Clock; nunca cambia el estado almacenado. */
    val atrasada: Boolean = false,
    val error: DomainError? = null,
    val isCompleting: Boolean = false,
    /** Error al marcar como completada (se muestra en un Snackbar). */
    val actionError: DomainError? = null,
    val isDeleting: Boolean = false,
    val isDeleted: Boolean = false,
    val deleteError: DomainError? = null
)
