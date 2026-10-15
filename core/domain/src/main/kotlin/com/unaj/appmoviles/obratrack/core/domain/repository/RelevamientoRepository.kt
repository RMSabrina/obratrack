package com.unaj.appmoviles.obratrack.core.domain.repository

import com.unaj.appmoviles.obratrack.core.domain.error.AppResult
import com.unaj.appmoviles.obratrack.core.domain.model.Relevamiento
import com.unaj.appmoviles.obratrack.core.domain.model.RelevamientoDraft
import kotlinx.coroutines.flow.Flow

interface RelevamientoRepository {
    fun observeAll(userId: String): Flow<List<Relevamiento>>
    fun observe(userId: String, obraId: String): Flow<List<Relevamiento>>
    fun observeByObra(obraId: String): Flow<List<Relevamiento>>
    suspend fun create(
        userId: String,
        obraId: String,
        draft: RelevamientoDraft,
        photo: ByteArray? = null
    ): AppResult<Relevamiento>
    suspend fun update(
        userId: String,
        obraId: String,
        relevamientoId: String,
        draft: RelevamientoDraft,
        photo: ByteArray? = null
    ): AppResult<Relevamiento>
}
