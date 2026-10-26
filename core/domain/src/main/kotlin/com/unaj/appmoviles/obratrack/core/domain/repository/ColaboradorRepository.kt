package com.unaj.appmoviles.obratrack.core.domain.repository

import com.unaj.appmoviles.obratrack.core.domain.model.Colaborador
import com.unaj.appmoviles.obratrack.core.domain.model.UserRole
import kotlinx.coroutines.flow.Flow

interface ColaboradorRepository {
    fun observeColaboradores(obraId: String): Flow<List<Colaborador>>
    suspend fun agregarColaborador(
        obraId: String,
        email: String,
        nombre: String,
        rol: UserRole
    ): Result<Colaborador>
    suspend fun removerColaborador(obraId: String, colaboradorId: String): Result<Unit>
}
