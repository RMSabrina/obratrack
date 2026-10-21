package com.unaj.appmoviles.obratrack.core.domain.usecase

import com.unaj.appmoviles.obratrack.core.domain.error.AppResult
import com.unaj.appmoviles.obratrack.core.domain.error.AuthError
import com.unaj.appmoviles.obratrack.core.domain.model.Credentials
import com.unaj.appmoviles.obratrack.core.domain.model.User
import com.unaj.appmoviles.obratrack.core.domain.repository.AuthRepository
import com.unaj.appmoviles.obratrack.core.domain.usecase.auth.ChangePasswordUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.auth.DeleteAccountUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.auth.ReauthenticateUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.auth.UpdateProfileNameUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthUseCasesTest {

    @Test
    fun updateProfileName_rechazaNombreVacio_yNoLlamaAlRepositorio() = runBlocking {
        val repository = FakeAuthRepository()
        val useCase = UpdateProfileNameUseCase(repository)

        val result = useCase("   ")

        assertEquals(AuthError.BlankName, (result as AppResult.Error).error)
        assertEquals(0, repository.updateProfileNameCalls)
    }

    @Test
    fun updateProfileName_recortaEspaciosYDelega() = runBlocking {
        val repository = FakeAuthRepository()
        val useCase = UpdateProfileNameUseCase(repository)

        val result = useCase("   Juan Pérez   ")

        assertTrue(result is AppResult.Success)
        assertEquals(1, repository.updateProfileNameCalls)
        assertEquals("Juan Pérez", repository.lastUpdatedName)
    }

    @Test
    fun changePassword_rechazaContrasenaCorta() = runBlocking {
        val repository = FakeAuthRepository()
        val useCase = ChangePasswordUseCase(repository)

        val result = useCase("12345")

        assertEquals(AuthError.PasswordTooShort, (result as AppResult.Error).error)
        assertEquals(0, repository.updatePasswordCalls)
    }

    @Test
    fun changePassword_aceptaContrasenaValida() = runBlocking {
        val repository = FakeAuthRepository()
        val useCase = ChangePasswordUseCase(repository)

        val result = useCase("123456")

        assertTrue(result is AppResult.Success)
        assertEquals(1, repository.updatePasswordCalls)
        assertEquals("123456", repository.lastPassword)
    }
}

private class FakeAuthRepository : AuthRepository {
    var updateProfileNameCalls = 0
    var updatePasswordCalls = 0
    var deleteAccountCalls = 0
    var reauthenticateCalls = 0
    var lastUpdatedName: String? = null
    var lastPassword: String? = null

    override val session: Flow<User?> = emptyFlow()

    override suspend fun login(credentials: Credentials): AppResult<User> =
        AppResult.Success(User("id", credentials.email, "User"))

    override suspend fun loginWithGoogle(idToken: String): AppResult<User> =
        AppResult.Success(User("google-id", "google@test.com", "Google User"))

    override suspend fun register(name: String, credentials: Credentials): AppResult<User> =
        AppResult.Success(User("id", credentials.email, name))

    override suspend fun logout() {}

    override suspend fun resendVerificationEmail(email: String, password: String): AppResult<Unit> =
        AppResult.Success(Unit)

    override suspend fun updateProfileName(newName: String): AppResult<User> {
        updateProfileNameCalls++
        lastUpdatedName = newName
        return AppResult.Success(User("id", "user@test.com", newName))
    }

    override suspend fun updatePassword(newPassword: String): AppResult<Unit> {
        updatePasswordCalls++
        lastPassword = newPassword
        return AppResult.Success(Unit)
    }

    override suspend fun deleteAccount(): AppResult<Unit> {
        deleteAccountCalls++
        return AppResult.Success(Unit)
    }

    override suspend fun reauthenticate(password: String): AppResult<Unit> {
        reauthenticateCalls++
        return AppResult.Success(Unit)
    }
}
