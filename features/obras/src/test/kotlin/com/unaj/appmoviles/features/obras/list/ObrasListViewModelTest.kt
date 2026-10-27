package com.unaj.appmoviles.features.obras.list

import com.unaj.appmoviles.features.obras.tareas.DUENO
import com.unaj.appmoviles.features.obras.tareas.FakeAuthRepository
import com.unaj.appmoviles.features.obras.tareas.FakeObraRepository
import com.unaj.appmoviles.features.obras.tareas.FakePendientesRepository
import com.unaj.appmoviles.features.obras.tareas.OBRA_ID
import com.unaj.appmoviles.features.obras.tareas.obraDe
import com.unaj.appmoviles.obratrack.core.domain.error.LecturaFallidaException
import com.unaj.appmoviles.obratrack.core.domain.error.ObraError
import com.unaj.appmoviles.obratrack.core.domain.usecase.auth.ObserveSessionUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.sync.ObservePendientesUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.ObserveObrasUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ObrasListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun crearViewModel(
        repo: FakeObraRepository,
        pendientes: FakePendientesRepository = FakePendientesRepository()
    ) = ObrasListViewModel(
        observeSession = ObserveSessionUseCase(FakeAuthRepository(DUENO)),
        observeObras = ObserveObrasUseCase(repo),
        observePendientes = ObservePendientesUseCase(pendientes)
    )

    @Test
    fun muestraLasObrasDelUsuario_sinError() = runTest {
        val viewModel = crearViewModel(FakeObraRepository(obraDe(DUENO.id)))

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf(OBRA_ID), state.obras.map { it.id })
        assertNull(state.error)
        assertFalse(state.isLoading)
    }

    @Test
    fun sinObras_noEsError() = runTest {
        val viewModel = crearViewModel(FakeObraRepository(null))

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.obras.isEmpty())
        assertNull(state.error)
        assertFalse(state.isLoading)
    }

    @Test
    fun errorAlLeer_muestraErrorYReintentarVuelveAMostrarLasObras() = runTest {
        val repo = FakeObraRepository(obraDe(DUENO.id)).apply {
            observeError = IllegalStateException("UNAVAILABLE")
        }
        val viewModel = crearViewModel(repo)
        testDispatcher.scheduler.advanceUntilIdle()

        val conError = viewModel.uiState.value
        assertEquals(ObraError.Desconocido, conError.error)
        assertTrue(conError.obras.isEmpty())
        assertFalse(conError.isLoading)

        repo.observeError = null
        viewModel.reintentar()
        assertTrue(viewModel.uiState.value.isLoading)
        testDispatcher.scheduler.advanceUntilIdle()

        val recuperado = viewModel.uiState.value
        assertNull(recuperado.error)
        assertEquals(listOf(OBRA_ID), recuperado.obras.map { it.id })
        assertFalse(recuperado.isLoading)
    }

    @Test
    fun conservaElErrorClasificadoPorLaCapaDeDatos() = runTest {
        val repo = FakeObraRepository(obraDe(DUENO.id)).apply {
            observeError = LecturaFallidaException(ObraError.Red, IllegalStateException("UNAVAILABLE"))
        }
        val viewModel = crearViewModel(repo)

        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(ObraError.Red, viewModel.uiState.value.error)
    }

    @Test
    fun sinReintentar_noVuelveALeerPorSuCuenta() = runTest {
        val repo = FakeObraRepository(obraDe(DUENO.id)).apply {
            observeError = IllegalStateException("UNAVAILABLE")
        }
        val viewModel = crearViewModel(repo)
        testDispatcher.scheduler.advanceUntilIdle()

        // Aunque el problema se arregle, la pantalla sigue en error hasta que se toque "Reintentar".
        repo.observeError = null
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(ObraError.Desconocido, viewModel.uiState.value.error)
    }

    @Test
    fun marcaLasObrasPendientes_yLasQuitaCuandoElServidorConfirma() = runTest {
        val pendientes = FakePendientesRepository(setOf(OBRA_ID))
        val viewModel = crearViewModel(FakeObraRepository(obraDe(DUENO.id)), pendientes)

        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(setOf(OBRA_ID), viewModel.uiState.value.pendientes)
        assertEquals(listOf(OBRA_ID), viewModel.uiState.value.obras.map { it.id })

        pendientes.ids.value = emptySet()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(emptySet<String>(), viewModel.uiState.value.pendientes)
        assertEquals(listOf(OBRA_ID), viewModel.uiState.value.obras.map { it.id })
    }
}
