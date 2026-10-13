package com.unaj.appmoviles.obratrack.core.data.repository

import com.unaj.appmoviles.obratrack.core.domain.error.AppResult
import com.unaj.appmoviles.obratrack.core.domain.model.Relevamiento
import com.unaj.appmoviles.obratrack.core.domain.model.RelevamientoDraft
import com.unaj.appmoviles.obratrack.core.domain.repository.RelevamientoRepository
import com.unaj.appmoviles.obratrack.core.firebase.FirebaseResult
import com.unaj.appmoviles.obratrack.core.firebase.FirestoreRelevamientoDataSource
import com.unaj.appmoviles.obratrack.core.firebase.RelevamientoDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RelevamientoRepositoryImpl(
    private val remoteDataSource: FirestoreRelevamientoDataSource
) : RelevamientoRepository {

    override fun observe(userId: String, obraId: String): Flow<List<Relevamiento>> =
        remoteDataSource.observe(userId, obraId).map { relevamientos ->
            relevamientos.map(RelevamientoDto::toDomain).sortedByDescending { it.fecha }
        }

    override suspend fun create(
        userId: String,
        obraId: String,
        draft: RelevamientoDraft,
        photo: ByteArray?
    ): AppResult<Relevamiento> =
        remoteDataSource.create(draft.toDto(userId, obraId), photo).toAppResult()

    override suspend fun update(
        userId: String,
        obraId: String,
        relevamientoId: String,
        draft: RelevamientoDraft,
        photo: ByteArray?
    ): AppResult<Relevamiento> =
        remoteDataSource.update(
            userId,
            draft.toDto(userId, obraId).copy(id = relevamientoId),
            photo
        ).toAppResult()

    private fun FirebaseResult<RelevamientoDto>.toAppResult(): AppResult<Relevamiento> = when (this) {
        is FirebaseResult.Success -> AppResult.Success(data.toDomain())
        is FirebaseResult.Error -> AppResult.Error(exception.toRelevamientoError())
    }
}
