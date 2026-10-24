package com.unaj.appmoviles.obratrack.core.domain.usecase.relevamientos

import com.unaj.appmoviles.obratrack.core.domain.model.Relevamiento
import com.unaj.appmoviles.obratrack.core.domain.repository.RelevamientoRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Relevamientos vinculados a una tarea. Solo para el dueño de la obra. */
class ObserveRelevamientosDeTareaUseCase @Inject constructor(
    private val relevamientoRepository: RelevamientoRepository
) {
    operator fun invoke(obraId: String, tareaId: String): Flow<List<Relevamiento>> =
        relevamientoRepository.observeByTarea(obraId, tareaId)
}
