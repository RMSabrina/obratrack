package com.unaj.appmoviles.obratrack.core.domain.usecase.tareas

import com.unaj.appmoviles.obratrack.core.domain.error.AppResult
import com.unaj.appmoviles.obratrack.core.domain.repository.TareaRepository
import javax.inject.Inject

class DeleteTareaUseCase @Inject constructor(
    private val tareaRepository: TareaRepository
) {
    suspend operator fun invoke(obraId: String, tareaId: String): AppResult<Unit> {
        return tareaRepository.delete(obraId, tareaId)
    }
}
