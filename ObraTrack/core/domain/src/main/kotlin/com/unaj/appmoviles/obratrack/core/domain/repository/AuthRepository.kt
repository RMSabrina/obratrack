package com.unaj.appmoviles.obratrack.core.domain.repository

import com.unaj.appmoviles.obratrack.core.domain.error.AppResult
import com.unaj.appmoviles.obratrack.core.domain.model.Credentials
import com.unaj.appmoviles.obratrack.core.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val session: Flow<User?>
    suspend fun login(credentials: Credentials): AppResult<User>
    suspend fun register(name: String, credentials: Credentials): AppResult<User>
    fun logout()
}