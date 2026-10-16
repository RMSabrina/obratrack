package com.unaj.appmoviles.features.obras.detail

import com.unaj.appmoviles.obratrack.core.domain.error.DomainError
import com.unaj.appmoviles.obratrack.core.domain.model.Obra
import com.unaj.appmoviles.obratrack.core.domain.model.Relevamiento

data class ObraDetailUiState(
    val isLoading: Boolean = true,
    val isEditable: Boolean = false,
    val obra: Obra? = null,
    val relevamientos: List<Relevamiento> = emptyList(),
    val error: DomainError? = null
)
