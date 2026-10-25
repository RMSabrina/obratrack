package com.unaj.appmoviles.obratrack.core.domain.usecase.obras

import com.unaj.appmoviles.obratrack.core.domain.error.AppResult
import com.unaj.appmoviles.obratrack.core.domain.model.Obra
import com.unaj.appmoviles.obratrack.core.domain.model.ObraDraft
import com.unaj.appmoviles.obratrack.core.domain.repository.ObraRepository
import javax.inject.Inject

class ScheduleVisitaUseCase @Inject constructor(
    private val obraRepository: ObraRepository
) {
    suspend operator fun invoke(userId: String, obra: Obra, proximaVisita: Long?): AppResult<Obra> {
        val draft = ObraDraft(
            nombre = obra.nombre,
            cliente = obra.cliente,
            direccion = obra.direccion,
            publica = obra.publica,
            estado = obra.estado,
            ubicacion = obra.ubicacion,
            proximaVisita = proximaVisita
        )
        return obraRepository.update(userId, obra.id, draft)
    }
}
