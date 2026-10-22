package com.unaj.appmoviles.obratrack.core.data.repository

import com.unaj.appmoviles.obratrack.core.domain.error.AppResult
import com.unaj.appmoviles.obratrack.core.domain.model.Tarea
import com.unaj.appmoviles.obratrack.core.domain.model.TareaDraft
import com.unaj.appmoviles.obratrack.core.domain.repository.TareaRepository
import com.unaj.appmoviles.obratrack.core.domain.usecase.tareas.ordenadas
import com.unaj.appmoviles.obratrack.core.firebase.FirebaseResult
import com.unaj.appmoviles.obratrack.core.firebase.FirestoreTareaDataSource
import com.unaj.appmoviles.obratrack.core.firebase.TareaDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TareaRepositoryImpl @Inject constructor(
    private val remoteDataSource: FirestoreTareaDataSource
) : TareaRepository {

    override fun observeByObra(obraId: String): Flow<List<Tarea>> =
        remoteDataSource.observeByObra(obraId).map { tareas ->
            tareas.map(TareaDto::toDomain).ordenadas()
        }

    override fun observeOne(obraId: String, tareaId: String): Flow<Tarea?> =
        remoteDataSource.observeOne(obraId, tareaId).map { it?.toDomain() }

    override suspend fun create(userId: String, obraId: String, draft: TareaDraft): AppResult<Tarea> =
        remoteDataSource.create(draft.toDto(userId, obraId)).toAppResult()

    override suspend fun update(
        userId: String,
        obraId: String,
        tareaId: String,
        draft: TareaDraft
    ): AppResult<Tarea> =
        remoteDataSource.update(userId, draft.toDto(userId, obraId).copy(id = tareaId)).toAppResult()

    override suspend fun delete(obraId: String, tareaId: String): AppResult<Unit> {
        return when (val res = remoteDataSource.delete(obraId, tareaId)) {
            is FirebaseResult.Success -> AppResult.Success(Unit)
            is FirebaseResult.Error -> AppResult.Error(res.exception.toTareaError())
        }
    }

    override suspend fun deleteAllByObra(obraId: String): AppResult<Unit> {
        return when (val res = remoteDataSource.deleteAllByObra(obraId)) {
            is FirebaseResult.Success -> AppResult.Success(Unit)
            is FirebaseResult.Error -> AppResult.Error(res.exception.toTareaError())
        }
    }

    private fun FirebaseResult<TareaDto>.toAppResult(): AppResult<Tarea> = when (this) {
        is FirebaseResult.Success -> AppResult.Success(data.toDomain())
        is FirebaseResult.Error -> AppResult.Error(exception.toTareaError())
    }
}
