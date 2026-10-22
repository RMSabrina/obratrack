package com.unaj.appmoviles.features.obras.tareas.form

import com.unaj.appmoviles.obratrack.core.domain.error.DomainError
import com.unaj.appmoviles.obratrack.core.domain.model.TareaDraft

data class TareaFormUiState(
    val isLoading: Boolean = true,
    /** Valores con los que arranca el formulario. Es null mientras carga o si no se pudo abrir. */
    val inicial: TareaDraft? = null,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val error: DomainError? = null
)
