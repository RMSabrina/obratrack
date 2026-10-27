package com.unaj.appmoviles.features.obras.tareas.cronograma

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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class CronogramaViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    // 22:00 del 21/10 en Buenos Aires = 01:00 UTC del 22/10. "Hoy" debe ser el 21.
    private val clockArgentina = Clock.fixed(
        Instant.parse("2026-10-22T01:00:00Z"),
        ZoneId.of("America/Argentina/Buenos_Aires")
    )
    private val hoy = LocalDate.of(2026, 10, 21)

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
        inicio: LocalDate? = null,
        fin: LocalDate? = null,
        estado: EstadoTarea = EstadoTarea.PENDIENTE
    ) = Tarea(
        id = id,
        obraId = OBRA_ID,
        userId = DUENO.id,
        titulo = "Tarea $id",
        fechaInicio = inicio,
        fechaFin = fin,
        estado = estado
    )

    private fun crearViewModel(
        user: User? = DUENO,
        obra: Obra? = obraDe(DUENO.id),
        tareaRepo: FakeTareaRepository = FakeTareaRepository()
    ) = CronogramaViewModel(
        obraId = OBRA_ID,
        observeSession = ObserveSessionUseCase(FakeAuthRepository(user)),
        observeObra = ObserveObraUseCase(FakeObraRepository(obra)),
        observeTareas = ObserveTareasUseCase(tareaRepo),
        clock = clockArgentina
    )

    @Test
    fun dueno_dibujaSoloLasPlanificadasYCuentaLasSinFechas() = runTest {
        val repo = FakeTareaRepository(
            listOf(
                tarea("b", hoy.plusDays(3), hoy.plusDays(5)),
                tarea("sin-fechas"),
                tarea("a", hoy.minusDays(2), hoy)
            )
        )
        val viewModel = crearViewModel(tareaRepo = repo)

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.error)
        val cronograma = state.cronograma!!
        assertEquals(listOf("a", "b"), cronograma.barras.map { it.tarea.id })
        assertEquals(1, cronograma.sinFechas)
    }

    @Test
    fun hoySaleDelClockEnLaZonaDelDispositivo() = runTest {
        val repo = FakeTareaRepository(
            listOf(
                tarea("fin-21", hoy.minusDays(3), hoy),
                tarea("fin-20", hoy.minusDays(3), hoy.minusDays(1))
            )
        )
        val viewModel = crearViewModel(tareaRepo = repo)

        testDispatcher.scheduler.advanceUntilIdle()

        val cronograma = viewModel.uiState.value.cronograma!!
        val atrasadas = cronograma.barras.associate { it.tarea.id to it.atrasada }
        // Si "hoy" se tomara en UTC (22/10), la del 21 figuraría atrasada.
        assertFalse(atrasadas.getValue("fin-21"))
        assertTrue(atrasadas.getValue("fin-20"))
        assertEquals(cronograma.periodo!!.indiceDe(hoy), cronograma.hoyDia)
    }

    @Test
    fun sinTareas_cronogramaVacioSinError() = runTest {
        val viewModel = crearViewModel()

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.error)
        val cronograma = state.cronograma!!
        assertTrue(cronograma.barras.isEmpty())
        assertEquals(0, cronograma.sinFechas)
        assertNull(cronograma.periodo)
    }

    @Test
    fun soloTareasSinFechas_noHayBarrasNiPeriodo() = runTest {
        val repo = FakeTareaRepository(listOf(tarea("a"), tarea("b")))
        val viewModel = crearViewModel(tareaRepo = repo)

        testDispatcher.scheduler.advanceUntilIdle()

        val cronograma = viewModel.uiState.value.cronograma!!
        assertTrue(cronograma.barras.isEmpty())
        assertNull(cronograma.periodo)
        assertEquals(2, cronograma.sinFechas)
    }

    @Test
    fun cambioDeFechasEnElRepositorio_actualizaLasBarras() = runTest {
        val repo = FakeTareaRepository(listOf(tarea("a", hoy, hoy)))
        val viewModel = crearViewModel(tareaRepo = repo)
        testDispatcher.scheduler.advanceUntilIdle()

        repo.tareas.value = listOf(tarea("a", hoy, hoy.plusDays(4)))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(5, viewModel.uiState.value.cronograma!!.barras.single().duracionDias)
    }

    @Test
    fun noModificaLasTareasAlmacenadas() = runTest {
        val vencida = tarea("vencida", hoy.minusDays(10), hoy.minusDays(5), EstadoTarea.EN_CURSO)
        val viewModel = crearViewModel(tareaRepo = FakeTareaRepository(listOf(vencida)))

        testDispatcher.scheduler.advanceUntilIdle()

        val barra = viewModel.uiState.value.cronograma!!.barras.single()
        assertTrue(barra.atrasada)
        assertEquals(vencida, barra.tarea)
    }

    @Test
    fun noDueno_obraPublica_noAutorizadoYNoConsultaTareas() = runTest {
        val repo = FakeTareaRepository(listOf(tarea("a", hoy, hoy)))
        val viewModel = crearViewModel(
            user = OTRO_USUARIO,
            obra = obraDe(DUENO.id, publica = true),
            tareaRepo = repo
        )

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(TareaError.NoAutorizada, state.error)
        assertNull(state.cronograma)
        assertEquals(0, repo.observeByObraCalls)
    }

    @Test
    fun sinSesion_noAutorizadoYNoConsultaTareas() = runTest {
        val repo = FakeTareaRepository(listOf(tarea("a", hoy, hoy)))
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
        assertNull(state.cronograma)
    }

    @Test
    fun reintentar_despuesDelErrorVuelveACalcularElCronograma() = runTest {
        val repo = FakeTareaRepository().apply { observeError = IllegalStateException("UNAVAILABLE") }
        val viewModel = crearViewModel(tareaRepo = repo)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(TareaError.Desconocido, viewModel.uiState.value.error)

        repo.observeError = null
        viewModel.reintentar()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.error)
        assertFalse(state.isLoading)
        assertNotNull(state.cronograma)
    }
}
