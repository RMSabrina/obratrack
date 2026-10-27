package com.unaj.appmoviles.obratrack.core.domain.usecase.obras

import com.unaj.appmoviles.obratrack.core.domain.model.PronosticoVisita
import com.unaj.appmoviles.obratrack.core.domain.repository.PronosticoRepository
import java.time.LocalDate
import javax.inject.Inject

class ObtenerPronosticoVisitaUseCase @Inject constructor(
    private val repository: PronosticoRepository
) {
    suspend operator fun invoke(
        latitud: Double,
        longitud: Double,
        fecha: LocalDate,
        hora: Int?
    ): PronosticoVisita = repository.obtenerParaVisita(latitud, longitud, fecha, hora)
}
