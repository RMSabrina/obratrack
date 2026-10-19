package com.unaj.appmoviles.features.obras.relevamiento.detail

import com.unaj.appmoviles.obratrack.core.domain.model.Obra
import com.unaj.appmoviles.obratrack.core.domain.model.Relevamiento

data class RelevamientoDetailUiState(
    val isLoading: Boolean = true,
    val isEditable: Boolean = false,
    val obra: Obra? = null,
    val relevamiento: Relevamiento? = null,
    val error: String? = null
)
