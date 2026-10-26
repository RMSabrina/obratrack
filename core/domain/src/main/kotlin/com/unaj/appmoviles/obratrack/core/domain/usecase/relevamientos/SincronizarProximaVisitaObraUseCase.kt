package com.unaj.appmoviles.obratrack.core.domain.usecase.relevamientos

import com.unaj.appmoviles.obratrack.core.domain.error.AppResult
import com.unaj.appmoviles.obratrack.core.domain.repository.ObraRepository
import com.unaj.appmoviles.obratrack.core.domain.repository.RelevamientoRepository
import javax.inject.Inject

/**
 * La próxima visita de la obra se copia de un relevamiento al guardarlo. Cuando ese relevamiento
 * se borra o pierde su próxima visita, la recalcula con los que siguen existiendo; si ninguno la
 * tiene, la obra queda sin próxima visita ni hora.
 */
class SincronizarProximaVisitaObraUseCase @Inject constructor(
    private val relevamientoRepository: RelevamientoRepository,
    private val obraRepository: ObraRepository
) {
    suspend operator fun invoke(obraId: String): AppResult<Unit> {
        val relevamientos = when (val res = relevamientoRepository.getByObra(obraId)) {
            is AppResult.Success -> res.data
            is AppResult.Error -> return res
        }
        val proxima = proximaVisitaDeObra(relevamientos)
        return obraRepository.setProximaVisita(obraId, proxima?.fecha, proxima?.hora)
    }
}
