package com.unaj.appmoviles.obratrack.core.domain.usecase.tareas

import com.unaj.appmoviles.obratrack.core.domain.model.Tarea
import com.unaj.appmoviles.obratrack.core.domain.repository.TareaRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveTareaUseCase @Inject constructor(
    private val tareaRepository: TareaRepository
) {
    operator fun invoke(obraId: String, tareaId: String): Flow<Tarea?> =
        tareaRepository.observeOne(obraId, tareaId)
}
