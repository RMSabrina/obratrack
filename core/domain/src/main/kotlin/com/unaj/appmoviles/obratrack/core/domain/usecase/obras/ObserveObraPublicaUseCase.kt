package com.unaj.appmoviles.obratrack.core.domain.usecase.obras

import com.unaj.appmoviles.obratrack.core.domain.model.Obra
import com.unaj.appmoviles.obratrack.core.domain.repository.ObraRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveObraPublicaUseCase @Inject constructor(
    private val obraRepository: ObraRepository
) {
    operator fun invoke(obraId: String): Flow<Obra?> =
        obraRepository.observePublica(obraId)
}
