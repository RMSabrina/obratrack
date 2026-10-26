package com.unaj.appmoviles.features.obras.detail

import com.unaj.appmoviles.features.obras.tareas.DUENO
import com.unaj.appmoviles.features.obras.tareas.FakeAuthRepository
import com.unaj.appmoviles.features.obras.tareas.FakeObraRepository
import com.unaj.appmoviles.features.obras.tareas.FakeRelevamientoRepository
import com.unaj.appmoviles.features.obras.tareas.FakeTareaRepository
import com.unaj.appmoviles.features.obras.tareas.OBRA_ID
import com.unaj.appmoviles.features.obras.tareas.OTRO_USUARIO
import com.unaj.appmoviles.features.obras.tareas.obraDe
import com.unaj.appmoviles.obratrack.core.domain.model.EstadoTarea
import com.unaj.appmoviles.obratrack.core.domain.model.Obra
import com.unaj.appmoviles.obratrack.core.domain.model.Relevamiento
import com.unaj.appmoviles.obratrack.core.domain.model.ResumenTareas
import com.unaj.appmoviles.obratrack.core.domain.model.Tarea
import com.unaj.appmoviles.obratrack.core.domain.model.User
import com.unaj.appmoviles.obratrack.core.domain.usecase.auth.ObserveSessionUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.DeleteObraUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.ObserveObraUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.relevamientos.ObserveRelevamientosUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.tareas.ObserveTareasUseCase
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
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class ObraDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    // 22:00 del 21/10 en Buenos Aires (01:00 UTC del 22): "hoy" es el 21.
    private val clock = Clock.fixed(
        Instant.parse("2026-10-22T01:00:00Z"),
        ZoneId.of("America/Argentina/Buenos_Aires")
    )

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

    private fun tarea(
        id: String,
        fechaFin: LocalDate? = null,
        estado: EstadoTarea = EstadoTarea.PENDIENTE
    ) = Tarea(
        id = id,
        obraId = OBRA_ID,
        userId = DUENO.id,
        titulo = "Tarea $id",
        fechaInicio = fechaFin?.minusDays(3),
        fechaFin = fechaFin,
        estado = estado
    )

    private fun crearViewModel(
        user: User? = DUENO,
        tareaRepo: FakeTareaRepository = FakeTareaRepository(),
        relevamientoRepo: FakeRelevamientoRepository = FakeRelevamientoRepository(listOf(relevamiento)),
        obra: Obra = obraDe(DUENO.id, publica = true)
    ): ObraDetailViewModel {
        val obraRepo = FakeObraRepository(obra)
        return ObraDetailViewModel(
            obraId = OBRA_ID,
            observeSession = ObserveSessionUseCase(FakeAuthRepository(user)),
            observeObra = ObserveObraUseCase(obraRepo),
            observeRelevamientos = ObserveRelevamientosUseCase(relevamientoRepo),
            observeTareas = ObserveTareasUseCase(tareaRepo),
            deleteObraUseCase = DeleteObraUseCase(obraRepo, relevamientoRepo, tareaRepo),
            clock = clock
        )
    }

    @Test
    fun dueno_veElResumenDeTareas_conAtrasadasDentroDePendientes() = runTest {
        val repo = FakeTareaRepository(
            listOf(
                tarea("completada", LocalDate.of(2026, 10, 1), EstadoTarea.COMPLETADA),
                tarea("sin-fechas"),
                tarea("fin-hoy", LocalDate.of(2026, 10, 21), EstadoTarea.EN_CURSO),
                tarea("fin-ayer", LocalDate.of(2026, 10, 20))
            )
        )
        val viewModel = crearViewModel(tareaRepo = repo)

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isEditable)
        assertEquals(ResumenTareas(completadas = 1, pendientes = 3, atrasadas = 1), state.resumenTareas)
        assertEquals(listOf(relevamiento), state.relevamientos)
    }

    private fun diaUtc(dia: LocalDate): Long = dia.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    private suspend fun proximaVisitaAtrasada(dia: LocalDate): Boolean {
        val obra = obraDe(DUENO.id, publica = true).copy(proximaVisita = diaUtc(dia))
        val viewModel = crearViewModel(obra = obra)
        testDispatcher.scheduler.advanceUntilIdle()
        return viewModel.uiState.value.proximaVisitaAtrasada
    }

    @Test
    fun proximaVisita_hoy_noEstaAtrasada_aunqueLaMedianocheUtcYaPaso() = runTest {
        // "Hoy" es el 21 en Buenos Aires (22:00 locales); la medianoche UTC del 21 ya quedó atrás.
        assertFalse(proximaVisitaAtrasada(LocalDate.of(2026, 10, 21)))
    }

    @Test
    fun proximaVisita_ayer_estaAtrasada() = runTest {
        assertTrue(proximaVisitaAtrasada(LocalDate.of(2026, 10, 20)))
    }

    @Test
    fun dueno_sinTareas_resumenEnCero() = runTest {
        val viewModel = crearViewModel()

        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(ResumenTareas(0, 0, 0), viewModel.uiState.value.resumenTareas)
    }

    @Test
    fun cambiosEnLasTareas_actualizanElResumen() = runTest {
        val repo = FakeTareaRepository(listOf(tarea("a")))
        val viewModel = crearViewModel(tareaRepo = repo)
        testDispatcher.scheduler.advanceUntilIdle()

        repo.tareas.value = listOf(tarea("a", estado = EstadoTarea.COMPLETADA), tarea("b"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(ResumenTareas(1, 1, 0), viewModel.uiState.value.resumenTareas)
    }

    @Test
    fun errorAlLeerTareas_ocultaElResumenSinAfectarLaObra() = runTest {
        val repo = FakeTareaRepository().apply { observeError = IllegalStateException("PERMISSION_DENIED") }
        val viewModel = crearViewModel(tareaRepo = repo)

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.resumenTareas)
        assertNull(state.error)
        assertTrue(state.isEditable)
        assertEquals(listOf(relevamiento), state.relevamientos)
    }

    @Test
    fun relevamientosCargados_sinError() = runTest {
        val viewModel = crearViewModel()

        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.relevamientosError)
    }

    @Test
    fun errorAlLeerRelevamientos_avisaSinOcultarLaObraNiLasTareas() = runTest {
        val relevamientoRepo = FakeRelevamientoRepository().apply {
            observeError = IllegalStateException("PERMISSION_DENIED")
        }
        val viewModel = crearViewModel(
            tareaRepo = FakeTareaRepository(listOf(tarea("a"))),
            relevamientoRepo = relevamientoRepo
        )

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.relevamientosError)
        assertTrue(state.relevamientos.isEmpty())
        assertNull(state.error)
        assertEquals(OBRA_ID, state.obra?.id)
        assertEquals(ResumenTareas(0, 1, 0), state.resumenTareas)
    }

    @Test
    fun noDueno_noVeResumenYNoConsultaTareas() = runTest {
        val repo = FakeTareaRepository(listOf(tarea("a")))
        val viewModel = crearViewModel(user = OTRO_USUARIO, tareaRepo = repo)

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isEditable)
        assertNull(state.resumenTareas)
        assertEquals(0, repo.observeByObraCalls)
    }
}
