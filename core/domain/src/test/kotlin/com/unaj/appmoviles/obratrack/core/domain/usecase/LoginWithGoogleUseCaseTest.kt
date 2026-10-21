package com.unaj.appmoviles.obratrack.core.domain.usecase

import com.unaj.appmoviles.obratrack.core.domain.error.AppResult
import com.unaj.appmoviles.obratrack.core.domain.error.AuthError
import com.unaj.appmoviles.obratrack.core.domain.model.Credentials
import com.unaj.appmoviles.obratrack.core.domain.model.User
import com.unaj.appmoviles.obratrack.core.domain.repository.AuthRepository
import com.unaj.appmoviles.obratrack.core.domain.usecase.auth.LoginWithGoogleUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LoginWithGoogleUseCaseTest {

    @Test
    fun loginWithGoogle_delegaAlRepositorio() = runBlocking {
        val repository = FakeGoogleAuthRepo()
        val useCase = LoginWithGoogleUseCase(repository)

        val result = useCase("sample-id-token")

        assertTrue(result is AppResult.Success)
        assertEquals("sample-id-token", repository.lastIdToken)
        assertEquals("google@test.com", (result as AppResult.Success).data.email)
    }

    @Test
    fun loginWithGoogle_propagaErrorDelRepositorio() = runBlocking {
        val repository = FakeGoogleAuthRepo(error = AuthError.GoogleSignInFailed)
        val useCase = LoginWithGoogleUseCase(repository)

        val result = useCase("invalid-token")

        assertTrue(result is AppResult.Error)
        assertEquals(AuthError.GoogleSignInFailed, (result as AppResult.Error).error)
    }
}

private class FakeGoogleAuthRepo(
    private val error: AuthError? = null
) : AuthRepository {
    var lastIdToken: String? = null

    override val session: Flow<User?> = emptyFlow()

    override suspend fun login(credentials: Credentials): AppResult<User> = error("Not needed")

    override suspend fun loginWithGoogle(idToken: String): AppResult<User> {
        lastIdToken = idToken
        return if (error != null) {
            AppResult.Error(error)
        } else {
            AppResult.Success(User("id-1", "google@test.com", "Google User"))
        }
    }

    override suspend fun register(name: String, credentials: Credentials): AppResult<User> = error("Not needed")
    override suspend fun logout() {}
    override suspend fun resendVerificationEmail(email: String, password: String): AppResult<Unit> = error("Not needed")
    override suspend fun updateProfileName(newName: String): AppResult<User> = error("Not needed")
    override suspend fun updatePassword(newPassword: String): AppResult<Unit> = error("Not needed")
    override suspend fun deleteAccount(): AppResult<Unit> = error("Not needed")
    override suspend fun reauthenticate(password: String): AppResult<Unit> = error("Not needed")
}
