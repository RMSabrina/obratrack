package com.unaj.appmoviles.obratrack.core.domain.usecase.sync

import com.unaj.appmoviles.obratrack.core.domain.model.MotivoFalloEscritura
import com.unaj.appmoviles.obratrack.core.domain.repository.FallosEscrituraRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveFallosEscrituraUseCase @Inject constructor(
    private val repository: FallosEscrituraRepository
) {
    operator fun invoke(): Flow<MotivoFalloEscritura> = repository.fallos
}
