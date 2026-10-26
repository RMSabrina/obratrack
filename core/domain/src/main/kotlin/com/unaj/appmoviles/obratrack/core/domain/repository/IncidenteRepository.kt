package com.unaj.appmoviles.obratrack.core.domain.repository

import com.unaj.appmoviles.obratrack.core.domain.model.Incidente
import kotlinx.coroutines.flow.Flow

interface IncidenteRepository {
    fun observeIncidentes(obraId: String): Flow<List<Incidente>>
    suspend fun reportarIncidente(incidente: Incidente): Result<Unit>
    suspend fun resolverIncidente(obraId: String, incidenteId: String): Result<Unit>
}
