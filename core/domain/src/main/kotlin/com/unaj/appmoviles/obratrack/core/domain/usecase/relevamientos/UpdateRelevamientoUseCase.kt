package com.unaj.appmoviles.obratrack.core.domain.usecase.relevamientos

import com.unaj.appmoviles.obratrack.core.domain.error.AppResult
import com.unaj.appmoviles.obratrack.core.domain.model.Relevamiento
import com.unaj.appmoviles.obratrack.core.domain.model.RelevamientoDraft
import com.unaj.appmoviles.obratrack.core.domain.repository.RelevamientoRepository
import javax.inject.Inject

class UpdateRelevamientoUseCase @Inject constructor(
    private val relevamientoRepository: RelevamientoRepository,
    private val sincronizarProximaVisita: SincronizarProximaVisitaObraUseCase
) {
    suspend operator fun invoke(
        userId: String,
        obraId: String,
        relevamientoId: String,
        draft: RelevamientoDraft,
        photo: ByteArray? = null
    ): AppResult<Relevamiento> {
        val normalized = draft.normalized()
        normalized.validationError()?.let { return AppResult.Error(it) }
        val result = relevamientoRepository.update(userId, obraId, relevamientoId, normalized, photo)
        // Sin próxima visita propia, la de la obra puede venir de otro relevamiento (o de ninguno).
        if (result is AppResult.Success && normalized.proximaVisita == null) {
            val sync = sincronizarProximaVisita(obraId)
            if (sync is AppResult.Error) return sync
        }
        return result
    }
}
