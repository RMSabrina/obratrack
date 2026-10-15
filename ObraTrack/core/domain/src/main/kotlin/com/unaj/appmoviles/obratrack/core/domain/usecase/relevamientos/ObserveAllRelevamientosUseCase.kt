package com.unaj.appmoviles.obratrack.core.domain.usecase.relevamientos

import com.unaj.appmoviles.obratrack.core.domain.model.Relevamiento
import com.unaj.appmoviles.obratrack.core.domain.repository.RelevamientoRepository
import kotlinx.coroutines.flow.Flow

class ObserveAllRelevamientosUseCase(
    private val relevamientoRepository: RelevamientoRepository
) {
    operator fun invoke(userId: String): Flow<List<Relevamiento>> =
        relevamientoRepository.observeAll(userId)
}
