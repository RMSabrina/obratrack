package com.unaj.appmoviles.obratrack.core.data.repository

import com.unaj.appmoviles.obratrack.core.domain.error.AppResult
import com.unaj.appmoviles.obratrack.core.domain.model.Credentials
import com.unaj.appmoviles.obratrack.core.domain.model.User
import com.unaj.appmoviles.obratrack.core.domain.repository.AuthRepository
import com.unaj.appmoviles.obratrack.core.firebase.FirebaseAuthDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val remoteDataSource: FirebaseAuthDataSource
) : AuthRepository {

    override val session: Flow<User?> = remoteDataSource.session.map { userDto ->
        userDto?.toDomain()
    }

    override suspend fun login(credentials: Credentials): AppResult<User> {
        val result = remoteDataSource.login(credentials.email, credentials.password)
        val mapped = result.toAppResult { it.toDomain() }
        android.util.Log.d("LOGIN", "raw=$result mapped=$mapped")
        return mapped
    }

    override suspend fun loginWithGoogle(idToken: String): AppResult<User> {
        val result = remoteDataSource.loginWithGoogle(idToken)
        return result.toAppResult { it.toDomain() }
    }

    override suspend fun register(name: String, credentials: Credentials): AppResult<User> {
        val result = remoteDataSource.register(name, credentials.email, credentials.password)
        return result.toAppResult { it.toDomain() }
    }

    override suspend fun logout() {
        remoteDataSource.logout()
    }

    override suspend fun resendVerificationEmail(email: String, password: String): AppResult<Unit> =
        remoteDataSource.resendVerificationEmail(email, password).toAppResult { it }

    override suspend fun updateProfileName(newName: String): AppResult<User> =
        remoteDataSource.updateProfileName(newName).toAppResult { it.toDomain() }

    override suspend fun updatePassword(newPassword: String): AppResult<Unit> =
        remoteDataSource.updatePassword(newPassword).toAppResult { it }

    override suspend fun deleteAccount(): AppResult<Unit> =
        remoteDataSource.deleteAccount().toAppResult { it }

    override suspend fun reauthenticate(password: String): AppResult<Unit> =
        remoteDataSource.reauthenticate(password).toAppResult { it }
}