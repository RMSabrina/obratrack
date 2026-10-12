package com.unaj.appmoviles.obratrack.core.domain.usecase

import com.unaj.appmoviles.obratrack.core.domain.model.Obra
import com.unaj.appmoviles.obratrack.core.domain.repository.ObraRepository
import kotlinx.coroutines.flow.Flow

class ObserveObrasUseCase(
    private val obraRepository: ObraRepository
) {
    operator fun invoke(userId: String): Flow<List<Obra>> = obraRepository.observe(userId)
}
