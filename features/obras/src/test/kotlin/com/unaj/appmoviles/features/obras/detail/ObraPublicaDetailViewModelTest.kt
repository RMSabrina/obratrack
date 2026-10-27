package com.unaj.appmoviles.features.obras.detail

import com.unaj.appmoviles.features.obras.tareas.DUENO
import com.unaj.appmoviles.features.obras.tareas.FakeObraRepository
import com.unaj.appmoviles.features.obras.tareas.FakeRelevamientoRepository
import com.unaj.appmoviles.features.obras.tareas.OBRA_ID
import com.unaj.appmoviles.features.obras.tareas.obraDe
import com.unaj.appmoviles.obratrack.core.domain.model.Relevamiento
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.ObserveObraPublicaUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.relevamientos.ObserveRelevamientosDeObraUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
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
class ObraPublicaDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val relevamiento = Relevamiento(
        id = "r1",
        obraId = OBRA_ID,
        userId = DUENO.id,
        fecha = 0L,
        nota = "Avance de obra"
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun crearViewModel(
        relevamientoRepo: FakeRelevamientoRepository,
        obraRepo: FakeObraRepository = FakeObraRepository(obraDe(DUENO.id, publica = true))
    ) =
        ObraPublicaDetailViewModel(
            obraId = OBRA_ID,
            observeObraPublicaUseCase = ObserveObraPublicaUseCase(obraRepo),
            observeRelevamientosUseCase = ObserveRelevamientosDeObraUseCase(relevamientoRepo)
        )

    /** uiState usa WhileSubscribed: sin un suscriptor no se calcula. */
    private fun TestScope.suscribir(viewModel: ObraPublicaDetailViewModel) {
        viewModel.uiState.launchIn(backgroundScope)
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @Test
    fun muestraLosRelevamientos_sinError() = runTest {
        val viewModel = crearViewModel(FakeRelevamientoRepository(listOf(relevamiento)))

        suscribir(viewModel)

        val state = viewModel.uiState.value
        assertEquals(listOf(relevamiento), state.relevamientos)
        assertFalse(state.relevamientosError)
    }

    @Test
    fun errorAlLeerRelevamientos_avisaYSigueMostrandoLaObra() = runTest {
        val relevamientoRepo = FakeRelevamientoRepository().apply {
            observeError = IllegalStateException("PERMISSION_DENIED")
        }
        val viewModel = crearViewModel(relevamientoRepo)

        suscribir(viewModel)

        val state = viewModel.uiState.value
        assertTrue(state.relevamientosError)
        assertTrue(state.relevamientos.isEmpty())
        assertFalse(state.isLoading)
        assertEquals(OBRA_ID, state.obra?.id)
    }

    @Test
    fun errorAlLeerLaObra_noCierraLaAppYAvisa() = runTest {
        val obraRepo = FakeObraRepository(obraDe(DUENO.id, publica = true)).apply {
            observePublicasError = IllegalStateException("PERMISSION_DENIED")
        }
        val viewModel = crearViewModel(FakeRelevamientoRepository(listOf(relevamiento)), obraRepo)

        suscribir(viewModel)

        val state = viewModel.uiState.value
        assertTrue(state.error)
        assertNull(state.obra)
        assertFalse(state.isLoading)
    }

    @Test
    fun obraQueNoEsPublica_noEsError() = runTest {
        val viewModel = crearViewModel(
            FakeRelevamientoRepository(),
            FakeObraRepository(obraDe(DUENO.id, publica = false))
        )

        suscribir(viewModel)

        val state = viewModel.uiState.value
        assertFalse(state.error)
        assertNull(state.obra)
    }
}
