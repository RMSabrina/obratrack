package com.unaj.appmoviles.obratrack.core.domain.repository

import com.unaj.appmoviles.obratrack.core.domain.error.AppResult
import com.unaj.appmoviles.obratrack.core.domain.model.RelevamientosVinculados
import com.unaj.appmoviles.obratrack.core.domain.model.Tarea
import com.unaj.appmoviles.obratrack.core.domain.model.TareaDraft
import kotlinx.coroutines.flow.Flow

interface TareaRepository {
    fun observeByObra(obraId: String): Flow<List<Tarea>>
    fun observeOne(obraId: String, tareaId: String): Flow<Tarea?>
    suspend fun create(userId: String, obraId: String, draft: TareaDraft): AppResult<Tarea>
    suspend fun update(
        userId: String,
        obraId: String,
        tareaId: String,
        draft: TareaDraft
    ): AppResult<Tarea>
    /** Borra la tarea y aplica [vinculados] a sus relevamientos, todo junto (un solo batch). */
    suspend fun delete(obraId: String, tareaId: String, vinculados: RelevamientosVinculados): AppResult<Unit>
    suspend fun deleteAllByObra(obraId: String): AppResult<Unit>
}
