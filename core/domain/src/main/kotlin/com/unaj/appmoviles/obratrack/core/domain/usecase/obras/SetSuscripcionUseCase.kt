package com.unaj.appmoviles.obratrack.core.domain.usecase.obras

import com.unaj.appmoviles.obratrack.core.domain.error.AppResult
import com.unaj.appmoviles.obratrack.core.domain.repository.ObraRepository
import javax.inject.Inject

class SetSuscripcionUseCase @Inject constructor(
    private val obraRepository: ObraRepository
) {
    suspend operator fun invoke(userId: String, obraId: String, seguir: Boolean): AppResult<Unit> =
        obraRepository.setSuscripcion(userId, obraId, seguir)
}
