package com.unaj.appmoviles.obratrack.core.firebase

import kotlinx.coroutines.flow.Flow

interface FirestoreObraDataSource {
    fun observe(userId: String): Flow<List<ObraDto>>
    fun observeOne(userId: String, obraId: String): Flow<ObraDto?>
    fun observePublicas(): Flow<List<ObraDto>>
    fun observePublica(obraId: String): Flow<ObraDto?>
    suspend fun create(dto: ObraDto): FirebaseResult<ObraDto>
    suspend fun update(userId: String, obraId: String, dto: ObraDto): FirebaseResult<ObraDto>
    suspend fun setProximaVisita(obraId: String, proximaVisitaMillis: Long?, horaVisita: Int?): FirebaseResult<Unit>
    suspend fun existsAndOwnedByCurrentUser(obraId: String): FirebaseResult<Boolean>
    suspend fun delete(obraId: String): FirebaseResult<Unit>
    suspend fun deleteAllSuscriptoresByObra(obraId: String): FirebaseResult<Unit>
}

class ObraNotFoundException : Exception("Obra no encontrada")

class ObraForbiddenException : Exception("La obra no pertenece al usuario")
