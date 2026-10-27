package com.unaj.appmoviles.obratrack.core.domain.usecase.obras

import com.unaj.appmoviles.obratrack.core.domain.repository.ObraRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveSuscripcionesUseCase @Inject constructor(
    private val obraRepository: ObraRepository
) {
    operator fun invoke(userId: String): Flow<Set<String>> = obraRepository.observeSuscripciones(userId)
}
