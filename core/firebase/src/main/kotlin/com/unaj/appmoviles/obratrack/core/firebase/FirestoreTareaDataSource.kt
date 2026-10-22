package com.unaj.appmoviles.obratrack.core.firebase

import kotlinx.coroutines.flow.Flow

interface FirestoreTareaDataSource {
    fun observeByObra(obraId: String): Flow<List<TareaDto>>
    fun observeOne(obraId: String, tareaId: String): Flow<TareaDto?>
    suspend fun create(dto: TareaDto): FirebaseResult<TareaDto>
    suspend fun update(userId: String, dto: TareaDto): FirebaseResult<TareaDto>
    suspend fun delete(obraId: String, tareaId: String): FirebaseResult<Unit>
    suspend fun deleteAllByObra(obraId: String): FirebaseResult<Unit>
}

class TareaNotFoundException : Exception("Tarea no encontrada")

class TareaForbiddenException : Exception("La tarea no pertenece al usuario")
