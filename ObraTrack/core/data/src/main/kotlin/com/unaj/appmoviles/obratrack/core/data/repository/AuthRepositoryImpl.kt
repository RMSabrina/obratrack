package com.unaj.appmoviles.obratrack.core.data.repository

import com.unaj.appmoviles.obratrack.core.domain.error.AppResult
import com.unaj.appmoviles.obratrack.core.domain.model.Credentials
import com.unaj.appmoviles.obratrack.core.domain.model.User
import com.unaj.appmoviles.obratrack.core.domain.repository.AuthRepository
import com.unaj.appmoviles.obratrack.core.firebase.FirebaseAuthDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AuthRepositoryImpl(
    private val remoteDataSource: FirebaseAuthDataSource
) : AuthRepository {

    override val session: Flow<User?> = remoteDataSource.session.map { userDto ->
        userDto?.toDomain()
    }

    override suspend fun login(credentials: Credentials): AppResult<User> {
        val result = remoteDataSource.login(credentials.email, credentials.password)
        return result.toAppResult { it.toDomain() }
    }

    override suspend fun register(name: String, credentials: Credentials): AppResult<User> {
        val result = remoteDataSource.register(name, credentials.email, credentials.password)
        return result.toAppResult { it.toDomain() }
    }

    override suspend fun logout() {
        remoteDataSource.logout()
    }
}