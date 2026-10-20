package com.unaj.appmoviles.obratrack.core.domain.usecase.relevamientos

import com.unaj.appmoviles.obratrack.core.domain.error.AppResult
import com.unaj.appmoviles.obratrack.core.domain.repository.RelevamientoRepository
import javax.inject.Inject

class DeleteRelevamientoUseCase @Inject constructor(
    private val relevamientoRepository: RelevamientoRepository
) {
    suspend operator fun invoke(obraId: String, relevamientoId: String): AppResult<Unit> {
        return relevamientoRepository.delete(obraId, relevamientoId)
    }
}
