package com.unaj.appmoviles.obratrack.core.domain.usecase.obras

import com.unaj.appmoviles.obratrack.core.domain.model.Obra
import com.unaj.appmoviles.obratrack.core.domain.repository.ObraRepository
import kotlinx.coroutines.flow.Flow

class ObserveObraUseCase(
    private val obraRepository: ObraRepository
) {
    operator fun invoke(userId: String, obraId: String): Flow<Obra?> =
        obraRepository.observeOne(userId, obraId)
}
