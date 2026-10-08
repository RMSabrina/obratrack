package com.unaj.appmoviles.obratrack.core.domain.repository

import com.unaj.appmoviles.obratrack.core.domain.error.AppResult
import com.unaj.appmoviles.obratrack.core.domain.model.Credentials
import com.unaj.appmoviles.obratrack.core.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun observeUser(): Flow<User?>
    suspend fun login(credentials: Credentials): AppResult<Unit>
    suspend fun register(name: String, credentials: Credentials): AppResult<Unit>
    suspend fun logout()
}