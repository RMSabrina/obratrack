package com.unaj.appmoviles.obratrack.core.domain.usecase.tareas

import com.unaj.appmoviles.obratrack.core.domain.error.AppResult
import com.unaj.appmoviles.obratrack.core.domain.model.Tarea
import com.unaj.appmoviles.obratrack.core.domain.model.TareaDraft
import com.unaj.appmoviles.obratrack.core.domain.repository.TareaRepository
import javax.inject.Inject

class CreateTareaUseCase @Inject constructor(
    private val tareaRepository: TareaRepository
) {
    suspend operator fun invoke(
        userId: String,
        obraId: String,
        draft: TareaDraft
    ): AppResult<Tarea> {
        val normalized = draft.normalized()
        normalized.validationError()?.let { return AppResult.Error(it) }
        return tareaRepository.create(userId, obraId, normalized)
    }
}
