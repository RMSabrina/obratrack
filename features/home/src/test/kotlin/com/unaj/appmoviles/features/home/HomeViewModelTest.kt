package com.unaj.appmoviles.features.home

import com.unaj.appmoviles.obratrack.core.domain.error.LecturaFallidaException
import com.unaj.appmoviles.obratrack.core.domain.error.ObraError
import com.unaj.appmoviles.obratrack.core.domain.model.CategoriaAgendaVisita
import com.unaj.appmoviles.obratrack.core.domain.model.Relevamiento
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.toMillisCalendario
import com.unaj.appmoviles.obratrack.core.domain.usecase.auth.ObserveSessionUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.ObserveAgendaVisitasUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.ObserveObrasUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.relevamientos.ObserveAllRelevamientosUseCase
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
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val clock = Clock.fixed(Instant.parse("2026-10-22T12:00:00Z"), ZoneOffset.UTC)

    private val relevamiento = Relevamiento(
        id = "r1",
        obraId = "obra-1",
        userId = USUARIO.id,
        fecha = 0L,
        nota = "Avance"
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
        obraRepo: FakeObraRepository,
        relevamientoRepo: FakeRelevamientoRepository = FakeRelevamientoRepository()
    ) = HomeViewModel(
        observeSessionUseCase = ObserveSessionUseCase(FakeAuthRepository(USUARIO)),
        observeObrasUseCase = ObserveObrasUseCase(obraRepo),
        observeAllRelevamientosUseCase = ObserveAllRelevamientosUseCase(relevamientoRepo),
        observeAgendaVisitasUseCase = ObserveAgendaVisitasUseCase(obraRepo, clock)
    )

    /** uiState usa WhileSubscribed: sin un suscriptor no se calcula. */
    private fun TestScope.suscribir(viewModel: HomeViewModel) {
        viewModel.uiState.launchIn(backgroundScope)
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @Test
    fun muestraLasObrasYLosRecientes_sinError() = runTest {
        val viewModel = crearViewModel(
            FakeObraRepository(listOf(obraDe())),
            FakeRelevamientoRepository(listOf(relevamiento))
        )

        suscribir(viewModel)

        val state = viewModel.uiState.value
        assertEquals(listOf("obra-1"), state.obras.map { it.id })
        assertEquals(listOf("r1"), state.recientes.map { it.relevamiento.id })
        assertNull(state.error)
        assertFalse(state.isLoading)
    }

    @Test
    fun sinObrasNiRelevamientos_noEsError() = runTest {
        val viewModel = crearViewModel(FakeObraRepository())

        suscribir(viewModel)

        val state = viewModel.uiState.value
        assertTrue(state.obras.isEmpty())
        assertTrue(state.recientes.isEmpty())
        assertNull(state.error)
        assertFalse(state.isLoading)
    }

    @Test
    fun errorAlLeerLasObras_muestraErrorYReintentarVuelveAMostrarLosDatos() = runTest {
        val obraRepo = FakeObraRepository(listOf(obraDe())).apply {
            observeError = IllegalStateException("UNAVAILABLE")
        }
        val viewModel = crearViewModel(obraRepo, FakeRelevamientoRepository(listOf(relevamiento)))
        suscribir(viewModel)

        val conError = viewModel.uiState.value
        assertEquals(ObraError.Desconocido, conError.error)
        assertTrue(conError.obras.isEmpty())
        assertFalse(conError.isLoading)

        obraRepo.observeError = null
        viewModel.reintentar()
        testDispatcher.scheduler.advanceUntilIdle()

        val recuperado = viewModel.uiState.value
        assertNull(recuperado.error)
        assertEquals(listOf("obra-1"), recuperado.obras.map { it.id })
        assertEquals(listOf("r1"), recuperado.recientes.map { it.relevamiento.id })
        assertFalse(recuperado.isLoading)
    }

    @Test
    fun conVisitasAtrasadas_laDestacadaEsLaMasAntigua_ynoLaDeHoy() = runTest {
        fun dia(d: Int) = LocalDate.of(2026, 10, d).toMillisCalendario()
        val obras = listOf(
            obraDe("hoy").copy(proximaVisita = dia(22)),
            obraDe("reciente").copy(proximaVisita = dia(20)),
            obraDe("antigua").copy(proximaVisita = dia(18))
        )
        val viewModel = crearViewModel(FakeObraRepository(obras))

        suscribir(viewModel)

        val state = viewModel.uiState.value
        assertEquals(2, state.totalVisitasAtrasadas)
        assertEquals(1, state.totalVisitasHoy)
        assertEquals("antigua", state.proximaVisitaDestacada?.obra?.id)
        assertEquals(CategoriaAgendaVisita.URGENTE_VENCIDA, state.proximaVisitaDestacada?.categoria)
    }

    @Test
    fun errorAlLeerLosRelevamientos_noSeMuestraComoSinRelevamientos() = runTest {
        val relevamientoRepo = FakeRelevamientoRepository(listOf(relevamiento)).apply {
            observeAllError = LecturaFallidaException(ObraError.Red, IllegalStateException("UNAVAILABLE"))
        }
        val viewModel = crearViewModel(FakeObraRepository(listOf(obraDe())), relevamientoRepo)

        suscribir(viewModel)

        assertEquals(ObraError.Red, viewModel.uiState.value.error)
    }
}
