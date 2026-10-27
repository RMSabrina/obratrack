package com.unaj.appmoviles.features.obras.list

import com.unaj.appmoviles.features.obras.tareas.DUENO
import com.unaj.appmoviles.features.obras.tareas.FakeAuthRepository
import com.unaj.appmoviles.features.obras.tareas.FakeObraRepository
import com.unaj.appmoviles.features.obras.tareas.OBRA_ID
import com.unaj.appmoviles.features.obras.tareas.obraDe
import com.unaj.appmoviles.obratrack.core.domain.usecase.auth.ObserveSessionUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.ObserveObrasPublicasUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.ObserveSuscripcionesUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.SetSuscripcionUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ObrasPublicasListViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun guardarYQuitarObra_actualizaElFiltroDeGuardadas() = runTest {
        val repository = FakeObraRepository(obra = null, publicas = listOf(obraDe("dueno", publica = true)))
        val viewModel = ObrasPublicasListViewModel(
            observeObrasPublicasUseCase = ObserveObrasPublicasUseCase(repository),
            observeSession = ObserveSessionUseCase(FakeAuthRepository(DUENO)),
            observeSuscripciones = ObserveSuscripcionesUseCase(repository),
            setSuscripcion = SetSuscripcionUseCase(repository)
        )
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(OBRA_ID in viewModel.uiState.value.suscripciones)

        viewModel.toggleSuscripcion(OBRA_ID)
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(OBRA_ID in viewModel.uiState.value.suscripciones)

        viewModel.toggleSuscripcion(OBRA_ID)
        dispatcher.scheduler.advanceUntilIdle()
        assertFalse(OBRA_ID in viewModel.uiState.value.suscripciones)
    }
}
