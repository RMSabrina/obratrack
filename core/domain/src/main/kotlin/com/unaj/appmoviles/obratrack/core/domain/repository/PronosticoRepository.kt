package com.unaj.appmoviles.obratrack.core.domain.repository

import com.unaj.appmoviles.obratrack.core.domain.model.PronosticoVisita
import java.time.LocalDate

interface PronosticoRepository {
    suspend fun obtenerParaVisita(
        latitud: Double,
        longitud: Double,
        fecha: LocalDate,
        hora: Int?
    ): PronosticoVisita
}
