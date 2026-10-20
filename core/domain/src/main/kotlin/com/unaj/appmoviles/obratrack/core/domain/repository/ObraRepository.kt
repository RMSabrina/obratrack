package com.unaj.appmoviles.obratrack.core.domain.repository

import com.unaj.appmoviles.obratrack.core.domain.error.AppResult
import com.unaj.appmoviles.obratrack.core.domain.model.Obra
import com.unaj.appmoviles.obratrack.core.domain.model.ObraDraft
import kotlinx.coroutines.flow.Flow

interface ObraRepository {
    fun observe(userId: String): Flow<List<Obra>>
    fun observeOne(userId: String, obraId: String): Flow<Obra?>
    fun observePublicas(): Flow<List<Obra>>
    fun observePublica(obraId: String): Flow<Obra?>
    suspend fun create(userId: String, draft: ObraDraft): AppResult<Obra>
    suspend fun update(userId: String, obraId: String, draft: ObraDraft): AppResult<Obra>
    suspend fun existsAndOwnedByCurrentUser(obraId: String): AppResult<Boolean>
    suspend fun delete(obraId: String): AppResult<Unit>
    suspend fun deleteAllSuscriptoresByObra(obraId: String): AppResult<Unit>
}
