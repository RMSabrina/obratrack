package com.unaj.appmoviles.obratrack.core.data.repository

import com.unaj.appmoviles.obratrack.core.domain.error.AppResult
import com.unaj.appmoviles.obratrack.core.domain.model.Obra
import com.unaj.appmoviles.obratrack.core.domain.model.ObraDraft
import com.unaj.appmoviles.obratrack.core.domain.repository.ObraRepository
import com.unaj.appmoviles.obratrack.core.firebase.FirebaseResult
import com.unaj.appmoviles.obratrack.core.firebase.FirestoreObraDataSource
import com.unaj.appmoviles.obratrack.core.firebase.ObraDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ObraRepositoryImpl @Inject constructor(
    private val remoteDataSource: FirestoreObraDataSource
) : ObraRepository {

    override fun observe(userId: String): Flow<List<Obra>> =
        remoteDataSource.observe(userId).map { obras ->
            obras.map(ObraDto::toDomain).sortedWith(OBRA_ORDER)
        }

    override fun observeOne(userId: String, obraId: String): Flow<Obra?> =
        remoteDataSource.observeOne(userId, obraId).map { it?.toDomain() }

    override fun observePublicas(): Flow<List<Obra>> =
        remoteDataSource.observePublicas().map { obras ->
            obras.map(ObraDto::toDomain).sortedWith(OBRA_ORDER)
        }

    override fun observePublica(obraId: String): Flow<Obra?> =
        remoteDataSource.observePublica(obraId).map { it?.toDomain() }

    override suspend fun create(userId: String, draft: ObraDraft): AppResult<Obra> {
        val dto = draft.toDto(userId = userId, fechaInicioMillis = System.currentTimeMillis())
        return remoteDataSource.create(dto).toAppResult()
    }

    override suspend fun update(userId: String, obraId: String, draft: ObraDraft): AppResult<Obra> {
        val dto = draft.toDto(userId = userId, fechaInicioMillis = 0L)
        return remoteDataSource.update(userId, obraId, dto).toAppResult()
    }

    private fun FirebaseResult<ObraDto>.toAppResult(): AppResult<Obra> = when (this) {
        is FirebaseResult.Success -> AppResult.Success(data.toDomain())
        is FirebaseResult.Error -> AppResult.Error(exception.toObraError())
    }

    private companion object {
        val OBRA_ORDER: Comparator<Obra> =
            compareByDescending<Obra> { it.ultimaVisita ?: Long.MIN_VALUE }
                .thenBy { it.nombre.lowercase() }
    }
}
