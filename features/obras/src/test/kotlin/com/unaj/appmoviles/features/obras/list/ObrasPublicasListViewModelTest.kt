package com.unaj.appmoviles.features.obras.list

import com.unaj.appmoviles.features.obras.tareas.DUENO
import com.unaj.appmoviles.features.obras.tareas.FakeObraRepository
import com.unaj.appmoviles.features.obras.tareas.OBRA_ID
import com.unaj.appmoviles.features.obras.tareas.obraDe
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.ObserveObrasPublicasUseCase
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ObrasPublicasListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** uiState usa WhileSubscribed: sin un suscriptor no se calcula. */
    private fun TestScope.suscribir(viewModel: ObrasPublicasListViewModel) {
        viewModel.uiState.launchIn(backgroundScope)
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @Test
    fun muestraLasObrasPublicas_sinError() = runTest {
        val viewModel = ObrasPublicasListViewModel(
            ObserveObrasPublicasUseCase(FakeObraRepository(obraDe(DUENO.id, publica = true)))
        )

        suscribir(viewModel)

        val state = viewModel.uiState.value
        assertEquals(listOf(OBRA_ID), state.obras.map { it.id })
        assertFalse(state.error)
        assertFalse(state.isLoading)
    }

    @Test
    fun errorAlLeer_noCierraLaAppYAvisa() = runTest {
        val repo = FakeObraRepository(obraDe(DUENO.id, publica = true)).apply {
            observePublicasError = IllegalStateException("PERMISSION_DENIED")
        }
        val viewModel = ObrasPublicasListViewModel(ObserveObrasPublicasUseCase(repo))

        suscribir(viewModel)

        val state = viewModel.uiState.value
        assertTrue(state.error)
        assertTrue(state.obras.isEmpty())
        assertFalse(state.isLoading)
    }
}
