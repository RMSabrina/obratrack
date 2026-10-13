package com.unaj.appmoviles.obratrack.core.domain.usecase.obras

import com.unaj.appmoviles.obratrack.core.domain.error.AppResult
import com.unaj.appmoviles.obratrack.core.domain.model.Obra
import com.unaj.appmoviles.obratrack.core.domain.model.ObraDraft
import com.unaj.appmoviles.obratrack.core.domain.repository.ObraRepository

class UpdateObraUseCase(
    private val obraRepository: ObraRepository
) {
    suspend operator fun invoke(userId: String, obraId: String, draft: ObraDraft): AppResult<Obra> {
        val normalized = draft.normalized()
        normalized.validationError()?.let { return AppResult.Error(it) }
        return obraRepository.update(userId, obraId, normalized)
    }
}