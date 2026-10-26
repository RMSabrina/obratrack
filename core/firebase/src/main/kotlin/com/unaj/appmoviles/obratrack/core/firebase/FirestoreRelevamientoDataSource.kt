package com.unaj.appmoviles.obratrack.core.firebase

import kotlinx.coroutines.flow.Flow

interface FirestoreRelevamientoDataSource {
    fun observeAll(userId: String): Flow<List<RelevamientoDto>>
    fun observe(userId: String, obraId: String): Flow<List<RelevamientoDto>>
    fun observeByObra(obraId: String): Flow<List<RelevamientoDto>>
    fun observeByTarea(obraId: String, tareaId: String): Flow<List<RelevamientoDto>>
    suspend fun create(dto: RelevamientoDto, photo: ByteArray? = null): FirebaseResult<RelevamientoDto>
    suspend fun update(userId: String, dto: RelevamientoDto, photo: ByteArray? = null): FirebaseResult<RelevamientoDto>
    suspend fun getByObra(obraId: String): FirebaseResult<List<RelevamientoDto>>
    suspend fun delete(obraId: String, relevamientoId: String): FirebaseResult<Unit>
    suspend fun deleteAllByObra(obraId: String): FirebaseResult<Unit>
}

class RelevamientoNotFoundException : Exception("Relevamiento no encontrado")

class RelevamientoForbiddenException : Exception("El relevamiento no pertenece al usuario")
