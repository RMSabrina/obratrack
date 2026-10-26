package com.unaj.appmoviles.obratrack.core.domain.usecase.relevamientos

import com.unaj.appmoviles.obratrack.core.domain.error.AppResult
import com.unaj.appmoviles.obratrack.core.domain.repository.RelevamientoRepository
import javax.inject.Inject

class DeleteRelevamientoUseCase @Inject constructor(
    private val relevamientoRepository: RelevamientoRepository,
    private val sincronizarProximaVisita: SincronizarProximaVisitaObraUseCase
) {
    suspend operator fun invoke(obraId: String, relevamientoId: String): AppResult<Unit> {
        val res = relevamientoRepository.delete(obraId, relevamientoId)
        if (res is AppResult.Error) return res
        return sincronizarProximaVisita(obraId)
    }
}
