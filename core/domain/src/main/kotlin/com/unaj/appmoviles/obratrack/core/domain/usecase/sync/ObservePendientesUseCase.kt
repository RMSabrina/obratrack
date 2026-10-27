package com.unaj.appmoviles.obratrack.core.domain.usecase.sync

import com.unaj.appmoviles.obratrack.core.domain.repository.PendientesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObservePendientesUseCase @Inject constructor(private val repository: PendientesRepository) {
    fun obras(userId: String): Flow<Set<String>> = repository.obras(userId)
    fun relevamientos(obraId: String): Flow<Set<String>> = repository.relevamientos(obraId)
    fun tareas(obraId: String): Flow<Set<String>> = repository.tareas(obraId)
}
