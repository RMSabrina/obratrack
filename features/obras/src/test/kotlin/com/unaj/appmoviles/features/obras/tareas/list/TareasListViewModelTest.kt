package com.unaj.appmoviles.features.obras.tareas.list

import com.unaj.appmoviles.features.obras.tareas.DUENO
import com.unaj.appmoviles.features.obras.tareas.FakeAuthRepository
import com.unaj.appmoviles.features.obras.tareas.FakeObraRepository
import com.unaj.appmoviles.features.obras.tareas.FakeTareaRepository
import com.unaj.appmoviles.features.obras.tareas.OBRA_ID
import com.unaj.appmoviles.features.obras.tareas.OTRO_USUARIO
import com.unaj.appmoviles.features.obras.tareas.obraDe
import com.unaj.appmoviles.obratrack.core.domain.error.TareaError
import com.unaj.appmoviles.obratrack.core.domain.model.EstadoTarea
import com.unaj.appmoviles.obratrack.core.domain.model.Obra
import com.unaj.appmoviles.obratrack.core.domain.model.Tarea
import com.unaj.appmoviles.obratrack.core.domain.model.User
import com.unaj.appmoviles.obratrack.core.domain.usecase.auth.ObserveSessionUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.ObserveObraUseCase
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

@OptIn(ExperimentalCoroutinesApi::class)
class TareasListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    // 22:00 del 21/10 en Buenos Aires = 01:00 UTC del 22/10. "Hoy" debe ser el 21.
    private val clockArgentina = Clock.fixed(
        Instant.parse("2026-10-22T01:00:00Z"),
        ZoneId.of("America/Argentina/Buenos_Aires")
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
        fechaInicio = fechaFin?.minusDays(5),
        fechaFin = fechaFin,
        estado = estado
    )

    private fun crearViewModel(
        user: User? = DUENO,
        obra: Obra? = obraDe(DUENO.id),
        tareaRepo: FakeTareaRepository = FakeTareaRepository()
    ) = TareasListViewModel(
        obraId = OBRA_ID,
        observeSession = ObserveSessionUseCase(FakeAuthRepository(user)),
        observeObra = ObserveObraUseCase(FakeObraRepository(obra)),
        observeTareas = ObserveTareasUseCase(tareaRepo),
        clock = clockArgentina
    )

    @Test
    fun dueno_veLasTareasEnElOrdenDelRepositorio() = runTest {
        val repo = FakeTareaRepository(listOf(tarea("a"), tarea("b"), tarea("c")))
        val viewModel = crearViewModel(tareaRepo = repo)

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals(listOf("a", "b", "c"), state.tareas.map { it.tarea.id })
    }

    @Test
    fun hoyEnArgentinaALas22_finEl21NoEstaAtrasada_finEl20Si() = runTest {
        val repo = FakeTareaRepository(
            listOf(
                tarea("fin-21", fechaFin = LocalDate.of(2026, 10, 21)),
                tarea("fin-20", fechaFin = LocalDate.of(2026, 10, 20))
            )
        )
        val viewModel = crearViewModel(tareaRepo = repo)

        testDispatcher.scheduler.advanceUntilIdle()

        val atrasadas = viewModel.uiState.value.tareas.associate { it.tarea.id to it.atrasada }
        // Si "hoy" se tomara en UTC (22/10), la del 21 figuraría atrasada.
        assertFalse(atrasadas.getValue("fin-21"))
        assertTrue(atrasadas.getValue("fin-20"))
    }

    @Test
    fun completadaConFinVencido_ySinFechas_noEstanAtrasadas() = runTest {
        val repo = FakeTareaRepository(
            listOf(
                tarea("completada", LocalDate.of(2026, 1, 1), EstadoTarea.COMPLETADA),
                tarea("sin-fechas")
            )
        )
        val viewModel = crearViewModel(tareaRepo = repo)

        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.tareas.none { it.atrasada })
    }

    @Test
    fun atrasada_noModificaElEstadoAlmacenado() = runTest {
        val vencida = tarea("vencida", LocalDate.of(2026, 1, 1), EstadoTarea.EN_CURSO)
        val viewModel = crearViewModel(tareaRepo = FakeTareaRepository(listOf(vencida)))

        testDispatcher.scheduler.advanceUntilIdle()

        val item = viewModel.uiState.value.tareas.single()
        assertTrue(item.atrasada)
        assertEquals(vencida, item.tarea)
    }

    @Test
    fun sinTareas_listaVaciaSinError() = runTest {
        val viewModel = crearViewModel()

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertTrue(state.tareas.isEmpty())
    }

    @Test
    fun cambiosEnElRepositorio_actualizanElListado() = runTest {
        val repo = FakeTareaRepository(listOf(tarea("a")))
        val viewModel = crearViewModel(tareaRepo = repo)
        testDispatcher.scheduler.advanceUntilIdle()

        repo.tareas.value = listOf(tarea("a"), tarea("b"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf("a", "b"), viewModel.uiState.value.tareas.map { it.tarea.id })
    }

    @Test
    fun noDueno_obraPublica_noAutorizadoYNoConsultaTareas() = runTest {
        val repo = FakeTareaRepository(listOf(tarea("a")))
        val viewModel = crearViewModel(
            user = OTRO_USUARIO,
            obra = obraDe(DUENO.id, publica = true),
            tareaRepo = repo
        )

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(TareaError.NoAutorizada, state.error)
        assertTrue(state.tareas.isEmpty())
        assertEquals(0, repo.observeByObraCalls)
    }

    @Test
    fun sinSesion_noAutorizado() = runTest {
        val repo = FakeTareaRepository(listOf(tarea("a")))
        val viewModel = crearViewModel(user = null, tareaRepo = repo)

        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(TareaError.NoAutorizada, viewModel.uiState.value.error)
        assertEquals(0, repo.observeByObraCalls)
    }

    @Test
    fun errorAlObservarTareas_muestraErrorDesconocido() = runTest {
        val repo = FakeTareaRepository().apply { observeError = IllegalStateException("PERMISSION_DENIED") }
        val viewModel = crearViewModel(tareaRepo = repo)

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(TareaError.Desconocido, state.error)
    }

    @Test
    fun reintentar_despuesDelErrorVuelveAMostrarLasTareas() = runTest {
        val repo = FakeTareaRepository(listOf(tarea("a"))).apply {
            observeError = IllegalStateException("UNAVAILABLE")
        }
        val viewModel = crearViewModel(tareaRepo = repo)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(TareaError.Desconocido, viewModel.uiState.value.error)

        repo.observeError = null
        viewModel.reintentar()
        assertTrue(viewModel.uiState.value.isLoading)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.error)
        assertFalse(state.isLoading)
        assertEquals(listOf("a"), state.tareas.map { it.tarea.id })
    }
}
