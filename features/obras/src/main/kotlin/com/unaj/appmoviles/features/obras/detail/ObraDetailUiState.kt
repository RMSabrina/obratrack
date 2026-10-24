package com.unaj.appmoviles.features.obras.detail

import com.unaj.appmoviles.obratrack.core.domain.error.DomainError
import com.unaj.appmoviles.obratrack.core.domain.model.Obra
import com.unaj.appmoviles.obratrack.core.domain.model.Relevamiento
import com.unaj.appmoviles.obratrack.core.domain.model.ResumenTareas

data class ObraDetailUiState(
    val isLoading: Boolean = true,
    val isEditable: Boolean = false,
    val obra: Obra? = null,
    val relevamientos: List<Relevamiento> = emptyList(),
    /** No se pudieron leer los relevamientos: la sección muestra un mensaje, no "vacío". */
    val relevamientosError: Boolean = false,
    /** Solo para el dueño; null si no es dueño o si no se pudieron leer las tareas. */
    val resumenTareas: ResumenTareas? = null,
    val error: DomainError? = null,
    val isDeleting: Boolean = false,
    val isDeleted: Boolean = false,
    val deleteError: DomainError? = null
)
