package com.unaj.appmoviles.features.auth.profile

import com.unaj.appmoviles.auth.R
import com.unaj.appmoviles.obratrack.core.domain.error.AppResult
import com.unaj.appmoviles.obratrack.core.domain.model.AppLanguage
import com.unaj.appmoviles.obratrack.core.domain.model.Credentials
import com.unaj.appmoviles.obratrack.core.domain.model.User
import com.unaj.appmoviles.obratrack.core.domain.repository.AuthRepository
import com.unaj.appmoviles.obratrack.core.domain.repository.LanguagePreferencesRepository
import com.unaj.appmoviles.obratrack.core.domain.usecase.auth.ChangePasswordUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.auth.DeleteAccountUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.auth.ObserveSessionUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.auth.ReauthenticateUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.auth.UpdateProfileNameUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val authRepo = FakeAuthRepoForProfile()
    private val languageRepo = FakeLanguageRepo()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun crearViewModel() = ProfileViewModel(
        ObserveSessionUseCase(authRepo),
        UpdateProfileNameUseCase(authRepo),
        ChangePasswordUseCase(authRepo),
        DeleteAccountUseCase(authRepo),
        ReauthenticateUseCase(authRepo),
        languageRepo
    )

    @Test
    fun saveName_exito_avisaConElTextoTraducible() = runTest(testDispatcher) {
        val viewModel = crearViewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onNameChange("Nombre Nuevo")
        advanceUntilIdle()
        viewModel.saveName()
        advanceUntilIdle()

        assertEquals(R.string.auth_nombre_actualizado, viewModel.uiState.value.successMessage)
    }

    @Test
    fun changePassword_exito_avisaConElTextoTraducible() = runTest(testDispatcher) {
        val viewModel = crearViewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.changePassword("clave-nueva")
        advanceUntilIdle()

        assertEquals(R.string.auth_password_actualizada, viewModel.uiState.value.successMessage)
    }

    @Test
    fun onLanguageChange_guardaYMuestraElIdiomaElegido() = runTest(testDispatcher) {
        val viewModel = crearViewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()
        assertEquals(AppLanguage.SPANISH, viewModel.uiState.value.selectedLanguage)

        viewModel.onLanguageChange(AppLanguage.ENGLISH)
        advanceUntilIdle()

        assertEquals(AppLanguage.ENGLISH, languageRepo.getLanguage())
        assertEquals(AppLanguage.ENGLISH, viewModel.uiState.value.selectedLanguage)
    }
}

private class FakeLanguageRepo : LanguagePreferencesRepository {
    private val idioma = MutableStateFlow(AppLanguage.SPANISH)
    override fun observeLanguage(): Flow<AppLanguage> = idioma
    override fun getLanguage(): AppLanguage = idioma.value
    override suspend fun setLanguage(language: AppLanguage) {
        idioma.value = language
    }
}

private class FakeAuthRepoForProfile : AuthRepository {
    override val session: Flow<User?> = MutableStateFlow(User("u1", "user@test.com", "Nombre"))

    override suspend fun updateProfileName(newName: String): AppResult<User> =
        AppResult.Success(User("u1", "user@test.com", newName))

    override suspend fun updatePassword(newPassword: String): AppResult<Unit> = AppResult.Success(Unit)
    override suspend fun deleteAccount(): AppResult<Unit> = AppResult.Success(Unit)
    override suspend fun reauthenticate(password: String): AppResult<Unit> = AppResult.Success(Unit)
    override suspend fun logout() {}
    override suspend fun login(credentials: Credentials): AppResult<User> = error("Not needed")
    override suspend fun loginWithGoogle(idToken: String): AppResult<User> = error("Not needed")
    override suspend fun register(name: String, credentials: Credentials): AppResult<User> = error("Not needed")
    override suspend fun resendVerificationEmail(email: String, password: String): AppResult<Unit> = error("Not needed")
}
