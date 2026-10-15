package com.unaj.appmoviles.features.home

import com.unaj.appmoviles.obratrack.core.domain.error.DomainError
import com.unaj.appmoviles.obratrack.core.domain.model.Obra
import com.unaj.appmoviles.obratrack.core.domain.model.Relevamiento

data class HomeUiState(
    val isLoading: Boolean = true,
    val userName: String = "",
    val obras: List<Obra> = emptyList(),
    val recientes: List<RelevamientoReciente> = emptyList(),
    val error: DomainError? = null
)

data class RelevamientoReciente(
    val relevamiento: Relevamiento,
    val obraNombre: String
)
