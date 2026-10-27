package com.unaj.appmoviles.obratrack.core.domain.usecase.sync

import com.unaj.appmoviles.obratrack.core.domain.repository.ConexionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveConexionUseCase @Inject constructor(private val repository: ConexionRepository) {
    operator fun invoke(): Flow<Boolean> = repository.enLinea
}
