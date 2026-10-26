package com.unaj.appmoviles.obratrack.core.domain.usecase.tareas

import com.unaj.appmoviles.obratrack.core.domain.error.AppResult
import com.unaj.appmoviles.obratrack.core.domain.model.RelevamientosVinculados
import com.unaj.appmoviles.obratrack.core.domain.repository.TareaRepository
import com.unaj.appmoviles.obratrack.core.domain.usecase.relevamientos.SincronizarProximaVisitaObraUseCase
import javax.inject.Inject

class DeleteTareaUseCase @Inject constructor(
    private val tareaRepository: TareaRepository,
    private val sincronizarProximaVisita: SincronizarProximaVisitaObraUseCase
) {
    suspend operator fun invoke(
        obraId: String,
        tareaId: String,
        vinculados: RelevamientosVinculados
    ): AppResult<Unit> {
        val res = tareaRepository.delete(obraId, tareaId, vinculados)
        if (res is AppResult.Error) return res
        // Si se borraron relevamientos, alguno pudo ser el que definía la próxima visita de la obra.
        if (vinculados == RelevamientosVinculados.ELIMINAR) return sincronizarProximaVisita(obraId)
        return res
    }
}
