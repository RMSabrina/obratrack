package com.unaj.appmoviles.features.auth.login

import com.unaj.appmoviles.obratrack.core.domain.error.AppResult
import com.unaj.appmoviles.obratrack.core.domain.error.AuthError
import com.unaj.appmoviles.obratrack.core.domain.model.Credentials
import com.unaj.appmoviles.obratrack.core.domain.model.User
import com.unaj.appmoviles.obratrack.core.domain.repository.AuthRepository
import com.unaj.appmoviles.obratrack.core.domain.usecase.auth.LoginUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.auth.LoginWithGoogleUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun onGoogleLoginClick_exito_transicionaAIdle() = runTest {
        val repo = FakeAuthRepoForVm()
        val loginUseCase = LoginUseCase(repo)
        val loginWithGoogleUseCase = LoginWithGoogleUseCase(repo)
        val viewModel = LoginViewModel(loginUseCase, loginWithGoogleUseCase)

        viewModel.onGoogleLoginClick("valid-token")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value is LoginUiState.Idle)
    }

    @Test
    fun onGoogleLoginClick_error_transicionaAError() = runTest {
        val repo = FakeAuthRepoForVm(googleError = AuthError.GoogleSignInFailed)
        val loginUseCase = LoginUseCase(repo)
        val loginWithGoogleUseCase = LoginWithGoogleUseCase(repo)
        val viewModel = LoginViewModel(loginUseCase, loginWithGoogleUseCase)

        viewModel.onGoogleLoginClick("invalid-token")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is LoginUiState.Error)
        assertEquals(AuthError.GoogleSignInFailed, (state as LoginUiState.Error).error)
    }

    @Test
    fun onGoogleLoginError_pasaAEstadoError() = runTest {
        val repo = FakeAuthRepoForVm()
        val loginUseCase = LoginUseCase(repo)
        val loginWithGoogleUseCase = LoginWithGoogleUseCase(repo)
        val viewModel = LoginViewModel(loginUseCase, loginWithGoogleUseCase)

        viewModel.onGoogleLoginError(AuthError.GoogleNoAccounts)

        val state = viewModel.uiState.value
        assertTrue(state is LoginUiState.Error)
        assertEquals(AuthError.GoogleNoAccounts, (state as LoginUiState.Error).error)
    }
}

private class FakeAuthRepoForVm(
    private val googleError: AuthError? = null
) : AuthRepository {
    override val session: Flow<User?> = emptyFlow()

    override suspend fun login(credentials: Credentials): AppResult<User> =
        AppResult.Success(User("id", credentials.email, "Name"))

    override suspend fun loginWithGoogle(idToken: String): AppResult<User> {
        return if (googleError != null) {
            AppResult.Error(googleError)
        } else {
            AppResult.Success(User("google-id", "google@test.com", "Google User"))
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
