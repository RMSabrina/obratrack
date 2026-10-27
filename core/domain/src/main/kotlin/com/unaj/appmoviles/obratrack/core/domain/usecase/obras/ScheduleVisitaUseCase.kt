package com.unaj.appmoviles.obratrack.core.domain.usecase.obras

import com.unaj.appmoviles.obratrack.core.domain.error.AppResult
import com.unaj.appmoviles.obratrack.core.domain.model.Obra
import com.unaj.appmoviles.obratrack.core.domain.model.ObraDraft
import com.unaj.appmoviles.obratrack.core.domain.repository.ObraRepository
import javax.inject.Inject

class ScheduleVisitaUseCase @Inject constructor(
    private val obraRepository: ObraRepository
) {
    /**
     * Programa o quita la próxima visita. [horaVisita] es la hora que queda (null = sin hora): quien
     * reprograma decide si conserva la que tenía la obra. Sin fecha no puede haber hora.
     * El aviso previo se reprograma solo: `AvisosVisitaSincronizador` mira los cambios de la obra.
     */
    suspend operator fun invoke(userId: String, obra: Obra, proximaVisita: Long?, horaVisita: Int?): AppResult<Obra> {
        val draft = ObraDraft(
            nombre = obra.nombre,
            cliente = obra.cliente,
            direccion = obra.direccion,
            publica = obra.publica,
            estado = obra.estado,
            ubicacion = obra.ubicacion,
            proximaVisita = proximaVisita,
            horaVisita = if (proximaVisita == null) null else horaVisitaValida(horaVisita)
        )
        return obraRepository.update(userId, obra.id, draft)
    }
}
