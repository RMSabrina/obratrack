package com.unaj.appmoviles.features.obras.list

import com.unaj.appmoviles.obratrack.core.domain.error.DomainError
import com.unaj.appmoviles.obratrack.core.domain.model.Obra

data class ObrasListUiState(
    val isLoading: Boolean = true,
    val obras: List<Obra> = emptyList(),
    val error: DomainError? = null,
    /** Ids de obras con cambios guardados en el teléfono que el servidor aún no confirmó. */
    val pendientes: Set<String> = emptySet()
)
