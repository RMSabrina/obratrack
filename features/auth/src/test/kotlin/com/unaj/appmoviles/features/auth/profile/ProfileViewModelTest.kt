package com.unaj.appmoviles.features.auth.profile

import com.unaj.appmoviles.auth.R
import com.unaj.appmoviles.obratrack.core.domain.error.AppResult
import com.unaj.appmoviles.obratrack.core.domain.model.AnticipacionAviso
import com.unaj.appmoviles.obratrack.core.domain.model.AppLanguage
import com.unaj.appmoviles.obratrack.core.domain.repository.AvisoPreferencesRepository
import com.unaj.appmoviles.obratrack.core.domain.model.Credentials
import com.unaj.appmoviles.obratrack.core.domain.model.User
import com.unaj.appmoviles.obratrack.core.domain.repository.AuthRepository
import com.unaj.appmoviles.obratrack.core.domain.repository.LanguagePreferencesRepository
import com.unaj.appmoviles.obratrack.core.domain.repository.ThemePreferencesRepository
import com.unaj.appmoviles.obratrack.core.domain.model.ModoTema
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
    private val avisoRepo = FakeAvisoRepo()
    private val temaRepo = FakeThemeRepo()

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
        languageRepo,
        avisoRepo,
        temaRepo
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
    fun onAnticipacionChange_guardaYMuestraLaAnticipacionElegida() = runTest(testDispatcher) {
        val viewModel = crearViewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()
        assertEquals(AnticipacionAviso.HORA_1, viewModel.uiState.value.selectedAnticipacion)

        viewModel.onAnticipacionChange(AnticipacionAviso.DIA_1)
        advanceUntilIdle()

        assertEquals(AnticipacionAviso.DIA_1, avisoRepo.getAnticipacion())
        assertEquals(AnticipacionAviso.DIA_1, viewModel.uiState.value.selectedAnticipacion)
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

    @Test
    fun onTemaChange_guardaYMuestraElModoElegido() = runTest(testDispatcher) {
        val viewModel = crearViewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()
        assertEquals(ModoTema.SISTEMA, viewModel.uiState.value.selectedTema)

        viewModel.onTemaChange(ModoTema.OSCURO)
        advanceUntilIdle()

        assertEquals(ModoTema.OSCURO, temaRepo.getModo())
        assertEquals(ModoTema.OSCURO, viewModel.uiState.value.selectedTema)

        viewModel.onTemaChange(ModoTema.SISTEMA)
        advanceUntilIdle()

        assertEquals(ModoTema.SISTEMA, viewModel.uiState.value.selectedTema)
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

private class FakeAvisoRepo : AvisoPreferencesRepository {
    private val anticipacion = MutableStateFlow(AnticipacionAviso.POR_DEFECTO)
    override fun observeAnticipacion(): Flow<AnticipacionAviso> = anticipacion
    override fun getAnticipacion(): AnticipacionAviso = anticipacion.value
    override suspend fun setAnticipacion(anticipacion: AnticipacionAviso) {
        this.anticipacion.value = anticipacion
    }
}

private class FakeThemeRepo : ThemePreferencesRepository {
    private val modo = MutableStateFlow(ModoTema.SISTEMA)
    override fun observeModo(): Flow<ModoTema> = modo
    override fun getModo(): ModoTema = modo.value
    override suspend fun setModo(modo: ModoTema) {
        this.modo.value = modo
    }
}
