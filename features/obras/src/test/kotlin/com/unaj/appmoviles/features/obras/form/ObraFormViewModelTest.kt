package com.unaj.appmoviles.features.obras.form

import com.unaj.appmoviles.features.obras.tareas.DUENO
import com.unaj.appmoviles.features.obras.tareas.FakeAuthRepository
import com.unaj.appmoviles.features.obras.tareas.obraDe
import com.unaj.appmoviles.obratrack.core.domain.error.AppResult
import com.unaj.appmoviles.obratrack.core.domain.model.EstadoObra
import com.unaj.appmoviles.obratrack.core.domain.model.Obra
import com.unaj.appmoviles.obratrack.core.domain.model.ObraDraft
import com.unaj.appmoviles.obratrack.core.domain.model.Ubicacion
import com.unaj.appmoviles.obratrack.core.domain.repository.GeocodingRepository
import com.unaj.appmoviles.obratrack.core.domain.repository.ObraRepository
import com.unaj.appmoviles.obratrack.core.domain.usecase.auth.ObserveSessionUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.location.GetAddressFromUbicacionUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.location.GetUbicacionFromAddressUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.CreateObraUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.UpdateObraUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ObraFormViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val authRepo = FakeAuthRepository(user = DUENO)
    private val obraRepo = object : ObraRepository {
        override fun observe(userId: String): Flow<List<Obra>> = emptyFlow()
        override fun observeOne(userId: String, obraId: String): Flow<Obra?> = emptyFlow()
        override fun observePublicas(): Flow<List<Obra>> = emptyFlow()
        override fun observePublica(obraId: String): Flow<Obra?> = emptyFlow()
        override fun observeSuscripciones(userId: String): Flow<Set<String>> = emptyFlow()
        override suspend fun setSuscripcion(userId: String, obraId: String, seguir: Boolean): AppResult<Unit> =
            AppResult.Success(Unit)
        override suspend fun create(userId: String, draft: ObraDraft): AppResult<Obra> =
            AppResult.Success(obraDe(userId))
        override suspend fun update(userId: String, obraId: String, draft: ObraDraft): AppResult<Obra> =
            AppResult.Success(obraDe(userId))
        override suspend fun existsAndOwnedByCurrentUser(obraId: String): AppResult<Boolean> =
            AppResult.Success(true)
        override suspend fun delete(obraId: String): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun deleteAllSuscriptoresByObra(obraId: String): AppResult<Unit> = AppResult.Success(Unit)
    }

    private val fakeGeocodingRepo = object : GeocodingRepository {
        val addressMap = mutableMapOf(
            "Av. San Martín 1500" to Ubicacion(-34.7963, -58.2764)
        )
        val locationMap = mutableMapOf(
            Pair(-34.7963, -58.2764) to "Av. San Martín 1500, Florencio Varela"
        )

        override suspend fun getUbicacionFromAddress(direccion: String): Ubicacion? {
            return addressMap[direccion]
        }

        override suspend fun getAddressFromUbicacion(latitud: Double, longitud: Double): String? {
            return locationMap[Pair(latitud, longitud)]
        }
    }

    private val observeSession = ObserveSessionUseCase(authRepo)
    private val createObra = CreateObraUseCase(obraRepo)
    private val updateObra = UpdateObraUseCase(obraRepo)
    private val getUbicacion = GetUbicacionFromAddressUseCase(fakeGeocodingRepo)
    private val getAddress = GetAddressFromUbicacionUseCase(fakeGeocodingRepo)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(obraId: String? = null) = ObraFormViewModel(
        observeSession = observeSession,
        createObra = createObra,
        updateObra = updateObra,
        getUbicacionFromAddress = getUbicacion,
        getAddressFromUbicacion = getAddress,
        obraId = obraId
    )

    @Test
    fun `estado inicial es Idle`() {
        val viewModel = createViewModel()
        assertTrue(viewModel.uiState.value is ObraFormUiState.Idle)
    }

    @Test
    fun `buscar ubicacion por direccion devuelve coordenadas correctas`() = runTest {
        val viewModel = createViewModel()
        val result = viewModel.buscarUbicacionDeDireccion("Av. San Martín 1500")
        assertEquals(Ubicacion(-34.7963, -58.2764), result)
    }

    @Test
    fun `buscar direccion por ubicacion devuelve texto formateado`() = runTest {
        val viewModel = createViewModel()
        val result = viewModel.buscarDireccionDeUbicacion(-34.7963, -58.2764)
        assertEquals("Av. San Martín 1500, Florencio Varela", result)
    }

    @Test
    fun `guardar obra nueva valida cambia estado a Saved`() = runTest {
        val viewModel = createViewModel()
        val draft = ObraDraft(
            nombre = "Edificio Centro",
            cliente = "Constructora Sur",
            direccion = "Av. San Martín 1500",
            publica = false,
            estado = EstadoObra.ACTIVA,
            ubicacion = Ubicacion(-34.7963, -58.2764)
        )

        viewModel.save(draft)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is ObraFormUiState.Saved)
    }
}
