package com.unaj.appmoviles.features.home.agenda

import com.unaj.appmoviles.features.home.FakeAuthRepository
import com.unaj.appmoviles.features.home.FakeObraRepository
import com.unaj.appmoviles.features.home.USUARIO
import com.unaj.appmoviles.features.home.obraDe
import com.unaj.appmoviles.obratrack.core.domain.error.ObraError
import com.unaj.appmoviles.obratrack.core.domain.usecase.auth.ObserveSessionUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.ObserveAgendaVisitasUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.ScheduleVisitaUseCase
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
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class AgendaViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val clock = Clock.fixed(Instant.parse("2026-10-22T12:00:00Z"), ZoneOffset.UTC)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun crearViewModel(obraRepo: FakeObraRepository) = AgendaViewModel(
        observeSessionUseCase = ObserveSessionUseCase(FakeAuthRepository(USUARIO)),
        observeAgendaVisitasUseCase = ObserveAgendaVisitasUseCase(obraRepo, clock),
        scheduleVisitaUseCase = ScheduleVisitaUseCase(obraRepo)
    )

    /** uiState usa WhileSubscribed: sin un suscriptor no se calcula. */
    private fun TestScope.suscribir(viewModel: AgendaViewModel) {
        viewModel.uiState.launchIn(backgroundScope)
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @Test
    fun muestraUnItemPorObra_sinError() = runTest {
        val viewModel = crearViewModel(FakeObraRepository(listOf(obraDe())))

        suscribir(viewModel)

        val state = viewModel.uiState.value
        assertEquals(listOf("obra-1"), state.items.map { it.obra.id })
        assertNull(state.error)
        assertFalse(state.isLoading)
    }

    @Test
    fun sinObras_agendaVaciaSinError() = runTest {
        val viewModel = crearViewModel(FakeObraRepository())

        suscribir(viewModel)

        val state = viewModel.uiState.value
        assertTrue(state.items.isEmpty())
        assertNull(state.error)
        assertFalse(state.isLoading)
    }

    @Test
    fun errorAlLeer_muestraErrorYReintentarVuelveAMostrarLaAgenda() = runTest {
        val obraRepo = FakeObraRepository(listOf(obraDe())).apply {
            observeError = IllegalStateException("UNAVAILABLE")
        }
        val viewModel = crearViewModel(obraRepo)
        suscribir(viewModel)

        val conError = viewModel.uiState.value
        assertEquals(ObraError.Desconocido, conError.error)
        assertTrue(conError.items.isEmpty())
        assertFalse(conError.isLoading)

        obraRepo.observeError = null
        viewModel.reintentar()
        testDispatcher.scheduler.advanceUntilIdle()

        val recuperado = viewModel.uiState.value
        assertNull(recuperado.error)
        assertEquals(listOf("obra-1"), recuperado.items.map { it.obra.id })
        assertFalse(recuperado.isLoading)
    }
}
