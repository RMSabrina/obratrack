package com.unaj.appmoviles.obratrack.core.domain.usecase.relevamientos

import com.unaj.appmoviles.obratrack.core.domain.error.AppResult
import com.unaj.appmoviles.obratrack.core.domain.model.Relevamiento
import com.unaj.appmoviles.obratrack.core.domain.model.RelevamientoDraft
import com.unaj.appmoviles.obratrack.core.domain.repository.RelevamientoRepository

class UpdateRelevamientoUseCase(
    private val relevamientoRepository: RelevamientoRepository
) {
    suspend operator fun invoke(
        userId: String,
        obraId: String,
        relevamientoId: String,
        draft: RelevamientoDraft
    ): AppResult<Relevamiento> {
        val normalized = draft.normalized()
        normalized.validationError()?.let { return AppResult.Error(it) }
        return relevamientoRepository.update(userId, obraId, relevamientoId, normalized)
    }
}
