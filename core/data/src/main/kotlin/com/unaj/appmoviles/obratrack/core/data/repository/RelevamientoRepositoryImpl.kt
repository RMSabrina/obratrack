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
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RelevamientoRepositoryImpl @Inject constructor(
    private val remoteDataSource: FirestoreRelevamientoDataSource,
    private val imageStorageRepository: com.unaj.appmoviles.obratrack.core.domain.repository.ImageStorageRepository
) : RelevamientoRepository {

    override fun observeAll(userId: String): Flow<List<Relevamiento>> =
        remoteDataSource.observeAll(userId).map { relevamientos ->
            relevamientos.map(RelevamientoDto::toDomain).sortedByDescending { it.fecha }
        }

    override fun observe(userId: String, obraId: String): Flow<List<Relevamiento>> =
        remoteDataSource.observe(userId, obraId).map { relevamientos ->
            relevamientos.map(RelevamientoDto::toDomain).sortedByDescending { it.fecha }
        }

    override fun observeByObra(obraId: String): Flow<List<Relevamiento>> =
        remoteDataSource.observeByObra(obraId).map { relevamientos ->
            relevamientos.map(RelevamientoDto::toDomain).sortedByDescending { it.fecha }
        }

    override fun observeByTarea(obraId: String, tareaId: String): Flow<List<Relevamiento>> =
        remoteDataSource.observeByTarea(obraId, tareaId).map { relevamientos ->
            relevamientos.map(RelevamientoDto::toDomain).sortedByDescending { it.fecha }
        }

    override suspend fun create(
        userId: String,
        obraId: String,
        draft: RelevamientoDraft,
        photo: ByteArray?
    ): AppResult<Relevamiento> {
        val fotoUrl = if (photo != null) {
            val result = imageStorageRepository.uploadImage(
                bytes = photo,
                folder = "relevamientos/$obraId",
                name = java.util.UUID.randomUUID().toString()
            )
            if (result is AppResult.Error) return AppResult.Error(result.error)
            (result as AppResult.Success).data
        } else {
            draft.fotoUrl
        }
        
        return remoteDataSource.create(draft.copy(fotoUrl = fotoUrl).toDto(userId, obraId), null).toAppResult()
    }

    override suspend fun update(
        userId: String,
        obraId: String,
        relevamientoId: String,
        draft: RelevamientoDraft,
        photo: ByteArray?
    ): AppResult<Relevamiento> {
        val fotoUrl = if (photo != null) {
            val result = imageStorageRepository.uploadImage(
                bytes = photo,
                folder = "relevamientos/$obraId",
                name = relevamientoId
            )
            if (result is AppResult.Error) return AppResult.Error(result.error)
            (result as AppResult.Success).data
        } else {
            draft.fotoUrl
        }
        
        return remoteDataSource.update(
            userId,
            draft.copy(fotoUrl = fotoUrl).toDto(userId, obraId).copy(id = relevamientoId),
            null
        ).toAppResult()
    }

    override suspend fun delete(obraId: String, relevamientoId: String): AppResult<Unit> {
        return when (val res = remoteDataSource.delete(obraId, relevamientoId)) {
            is FirebaseResult.Success -> AppResult.Success(Unit)
            is FirebaseResult.Error -> AppResult.Error(res.exception.toRelevamientoError())
        }
    }

    override suspend fun deleteAllByObra(obraId: String): AppResult<Unit> {
        return when (val res = remoteDataSource.deleteAllByObra(obraId)) {
            is FirebaseResult.Success -> AppResult.Success(Unit)
            is FirebaseResult.Error -> AppResult.Error(res.exception.toRelevamientoError())
        }
    }

    private fun FirebaseResult<RelevamientoDto>.toAppResult(): AppResult<Relevamiento> = when (this) {
        is FirebaseResult.Success -> AppResult.Success(data.toDomain())
        is FirebaseResult.Error -> AppResult.Error(exception.toRelevamientoError())
    }
}
