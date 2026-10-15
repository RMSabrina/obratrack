package com.unaj.appmoviles.obratrack.core.domain.usecase.relevamientos

import com.unaj.appmoviles.obratrack.core.domain.model.Relevamiento
import com.unaj.appmoviles.obratrack.core.domain.repository.RelevamientoRepository
import kotlinx.coroutines.flow.Flow

class ObserveRelevamientosDeObraUseCase(
    private val relevamientoRepository: RelevamientoRepository
) {
    operator fun invoke(obraId: String): Flow<List<Relevamiento>> =
        relevamientoRepository.observeByObra(obraId)
}
