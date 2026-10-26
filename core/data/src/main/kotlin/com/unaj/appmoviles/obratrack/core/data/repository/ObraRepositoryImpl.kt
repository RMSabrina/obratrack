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
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.toMillisCalendario
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ObraRepositoryImpl @Inject constructor(
    private val remoteDataSource: FirestoreObraDataSource,
    private val clock: Clock
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
        val dto = draft.toDto(userId = userId, fechaInicioMillis = LocalDate.now(clock).toMillisCalendario())
        return remoteDataSource.create(dto).toAppResult()
    }

    override suspend fun update(userId: String, obraId: String, draft: ObraDraft): AppResult<Obra> {
        val dto = draft.toDto(userId = userId, fechaInicioMillis = 0L)
        return remoteDataSource.update(userId, obraId, dto).toAppResult()
    }

    override suspend fun setProximaVisita(obraId: String, proximaVisita: Long?, horaVisita: Int?): AppResult<Unit> {
        return when (val res = remoteDataSource.setProximaVisita(obraId, proximaVisita, horaVisita)) {
            is FirebaseResult.Success -> AppResult.Success(Unit)
            is FirebaseResult.Error -> AppResult.Error(res.exception.toObraError())
        }
    }

    override suspend fun existsAndOwnedByCurrentUser(obraId: String): AppResult<Boolean> {
        return when (val res = remoteDataSource.existsAndOwnedByCurrentUser(obraId)) {
            is FirebaseResult.Success -> AppResult.Success(res.data)
            is FirebaseResult.Error -> AppResult.Error(res.exception.toObraError())
        }
    }

    override suspend fun delete(obraId: String): AppResult<Unit> {
        return when (val res = remoteDataSource.delete(obraId)) {
            is FirebaseResult.Success -> AppResult.Success(Unit)
            is FirebaseResult.Error -> AppResult.Error(res.exception.toObraError())
        }
    }

    override suspend fun deleteAllSuscriptoresByObra(obraId: String): AppResult<Unit> {
        return when (val res = remoteDataSource.deleteAllSuscriptoresByObra(obraId)) {
            is FirebaseResult.Success -> AppResult.Success(Unit)
            is FirebaseResult.Error -> AppResult.Error(res.exception.toObraError())
        }
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
