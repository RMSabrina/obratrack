package com.unaj.appmoviles.obratrack.core.data.repository

import com.unaj.appmoviles.obratrack.core.domain.error.AppResult
import com.unaj.appmoviles.obratrack.core.domain.repository.FotoRelevamientoRepository
import com.unaj.appmoviles.obratrack.core.firebase.FirebaseResult
import com.unaj.appmoviles.obratrack.core.firebase.FirestoreFotoDataSource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FotoRelevamientoRepositoryImpl @Inject constructor(
    private val dataSource: FirestoreFotoDataSource
) : FotoRelevamientoRepository {

    override suspend fun existe(obraId: String, relevamientoId: String): AppResult<Boolean> =
        dataSource.existe(obraId, relevamientoId).aAppResult()

    override suspend fun asignarFotoUrl(obraId: String, relevamientoId: String, url: String): AppResult<Boolean> =
        dataSource.asignarFotoUrl(obraId, relevamientoId, url).aAppResult()

    private fun FirebaseResult<Boolean>.aAppResult(): AppResult<Boolean> = when (this) {
        is FirebaseResult.Success -> AppResult.Success(data)
        is FirebaseResult.Error -> AppResult.Error(exception.toRelevamientoError())
    }
}
