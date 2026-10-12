package com.unaj.appmoviles.obratrack.core.firebase

import kotlinx.coroutines.flow.Flow

interface FirestoreObraDataSource {
    fun observe(userId: String): Flow<List<ObraDto>>
    suspend fun create(dto: ObraDto): FirebaseResult<ObraDto>
    suspend fun update(userId: String, obraId: String, dto: ObraDto): FirebaseResult<ObraDto>
}

class ObraNotFoundException : Exception("Obra no encontrada")

class ObraForbiddenException : Exception("La obra no pertenece al usuario")
