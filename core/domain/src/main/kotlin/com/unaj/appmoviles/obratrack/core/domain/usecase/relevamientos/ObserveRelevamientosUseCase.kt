package com.unaj.appmoviles.obratrack.core.domain.usecase.relevamientos

import com.unaj.appmoviles.obratrack.core.domain.model.Relevamiento
import com.unaj.appmoviles.obratrack.core.domain.repository.RelevamientoRepository
import kotlinx.coroutines.flow.Flow

class ObserveRelevamientosUseCase(
    private val relevamientoRepository: RelevamientoRepository
) {
    operator fun invoke(userId: String, obraId: String): Flow<List<Relevamiento>> =
        relevamientoRepository.observe(userId, obraId)
}
