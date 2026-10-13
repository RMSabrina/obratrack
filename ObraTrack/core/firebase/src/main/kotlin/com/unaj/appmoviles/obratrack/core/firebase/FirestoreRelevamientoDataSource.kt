package com.unaj.appmoviles.obratrack.core.firebase

import kotlinx.coroutines.flow.Flow

interface FirestoreRelevamientoDataSource {
    fun observe(userId: String, obraId: String): Flow<List<RelevamientoDto>>
    suspend fun create(dto: RelevamientoDto, photo: ByteArray? = null): FirebaseResult<RelevamientoDto>
    suspend fun update(userId: String, dto: RelevamientoDto, photo: ByteArray? = null): FirebaseResult<RelevamientoDto>
}

class RelevamientoNotFoundException : Exception("Relevamiento no encontrado")

class RelevamientoForbiddenException : Exception("El relevamiento no pertenece al usuario")
