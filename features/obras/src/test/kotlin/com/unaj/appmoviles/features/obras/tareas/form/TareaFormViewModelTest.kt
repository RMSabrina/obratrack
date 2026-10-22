package com.unaj.appmoviles.features.obras.tareas.form

import com.unaj.appmoviles.features.obras.tareas.DUENO
import com.unaj.appmoviles.features.obras.tareas.FakeAuthRepository
import com.unaj.appmoviles.features.obras.tareas.FakeObraRepository
import com.unaj.appmoviles.features.obras.tareas.FakeTareaRepository
import com.unaj.appmoviles.features.obras.tareas.OBRA_ID
import com.unaj.appmoviles.features.obras.tareas.OTRO_USUARIO
import com.unaj.appmoviles.features.obras.tareas.obraDe
import com.unaj.appmoviles.obratrack.core.domain.error.TareaError
import com.unaj.appmoviles.obratrack.core.domain.model.CategoriaTarea
import com.unaj.appmoviles.obratrack.core.domain.model.EstadoTarea
import com.unaj.appmoviles.obratrack.core.domain.model.Tarea
import com.unaj.appmoviles.obratrack.core.domain.model.TareaDraft
import com.unaj.appmoviles.obratrack.core.domain.model.User
import com.unaj.appmoviles.obratrack.core.domain.usecase.auth.ObserveSessionUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.ObserveObraUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.tareas.CreateTareaUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.tareas.ObserveTareaUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.tareas.UpdateTareaUseCase
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
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class TareaFormViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val existente = Tarea(
        id = "t1",
        obraId = OBRA_ID,
        userId = DUENO.id,
        titulo = "Instalar tablero",
        descripcion = "Tablero principal",
        categoria = CategoriaTarea.ELECTRICIDAD,
        fechaInicio = LocalDate.of(2026, 10, 1),
        fechaFin = LocalDate.of(2026, 10, 10),
        estado = EstadoTarea.EN_CURSO
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
        tareaId: String? = null,
        user: User? = DUENO,
        tareaRepo: FakeTareaRepository = FakeTareaRepository(listOf(existente))
    ) = TareaFormViewModel(
        obraId = OBRA_ID,
        tareaId = tareaId,
        observeSession = ObserveSessionUseCase(FakeAuthRepository(user)),
        observeObra = ObserveObraUseCase(FakeObraRepository(obraDe(DUENO.id, publica = true))),
        observeTarea = ObserveTareaUseCase(tareaRepo),
        createTarea = CreateTareaUseCase(tareaRepo),
        updateTarea = UpdateTareaUseCase(tareaRepo)
    )

    @Test
    fun nueva_arrancaConFormularioVacio() = runTest {
        val viewModel = crearViewModel()

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals(TareaDraft(titulo = ""), state.inicial)
    }

    @Test
    fun nueva_guardar_creaConElDuenoYTerminaGuardada() = runTest {
        val repo = FakeTareaRepository()
        val viewModel = crearViewModel(tareaRepo = repo)
        testDispatcher.scheduler.advanceUntilIdle()
        val draft = TareaDraft(
            titulo = "  Pintar fachada  ",
            categoria = CategoriaTarea.PINTURA,
            fechaInicio = LocalDate.of(2026, 11, 1),
            fechaFin = LocalDate.of(2026, 11, 3)
        )

        viewModel.save(draft)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isSaved)
        assertEquals(1, repo.created.size)
        val (userId, guardado) = repo.created.single()
        assertEquals(DUENO.id, userId)
        assertEquals(draft.copy(titulo = "Pintar fachada"), guardado)
    }

    @Test
    fun editar_cargaLaTareaPorId() = runTest {
        val viewModel = crearViewModel(tareaId = existente.id)

        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            TareaDraft(
                titulo = existente.titulo,
                descripcion = existente.descripcion,
                categoria = existente.categoria,
                fechaInicio = existente.fechaInicio,
                fechaFin = existente.fechaFin,
                estado = existente.estado
            ),
            viewModel.uiState.value.inicial
        )
    }

    @Test
    fun editar_quitandoFechas_actualizaLaTarea() = runTest {
        val repo = FakeTareaRepository(listOf(existente))
        val viewModel = crearViewModel(tareaId = existente.id, tareaRepo = repo)
        testDispatcher.scheduler.advanceUntilIdle()
        val sinFechas = viewModel.uiState.value.inicial!!.copy(fechaInicio = null, fechaFin = null)

        viewModel.save(sinFechas)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isSaved)
        assertTrue(repo.created.isEmpty())
        assertEquals(Triple(DUENO.id, existente.id, sinFechas), repo.updated.single())
    }

    @Test
    fun editar_tareaInexistente_noEncontrada() = runTest {
        val viewModel = crearViewModel(tareaId = "no-existe")

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(TareaError.NoEncontrada, state.error)
        assertNull(state.inicial)
    }

    @Test
    fun tituloVacio_muestraErrorYNoGuarda() = runTest {
        val repo = FakeTareaRepository()
        val viewModel = crearViewModel(tareaRepo = repo)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.save(TareaDraft(titulo = "   "))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(TareaError.TituloVacio, state.error)
        assertFalse(state.isSaving)
        assertFalse(state.isSaved)
        assertTrue(repo.created.isEmpty())
    }

    @Test
    fun soloUnaFecha_muestraFechasIncompletas() = runTest {
        val viewModel = crearViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.save(TareaDraft(titulo = "Losa", fechaInicio = LocalDate.of(2026, 11, 1)))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(TareaError.FechasIncompletas, viewModel.uiState.value.error)
    }

    @Test
    fun finAnteriorAlInicio_muestraFechaFinAnterior() = runTest {
        val viewModel = crearViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.save(
            TareaDraft(
                titulo = "Losa",
                fechaInicio = LocalDate.of(2026, 11, 10),
                fechaFin = LocalDate.of(2026, 11, 1)
            )
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(TareaError.FechaFinAnterior, viewModel.uiState.value.error)
    }

    @Test
    fun onInputChanged_limpiaElErrorDeValidacion() = runTest {
        val viewModel = crearViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.save(TareaDraft(titulo = ""))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onInputChanged()

        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun errorDelRepositorio_seMuestraYPermiteReintentar() = runTest {
        val repo = FakeTareaRepository().apply { writeError = TareaError.Red }
        val viewModel = crearViewModel(tareaRepo = repo)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.save(TareaDraft(titulo = "Losa"))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(TareaError.Red, state.error)
        assertFalse(state.isSaving)
        assertFalse(state.isSaved)
    }

    @Test
    fun noDueno_noAutorizadoYNoPuedeGuardar() = runTest {
        val repo = FakeTareaRepository(listOf(existente))
        val viewModel = crearViewModel(tareaId = existente.id, user = OTRO_USUARIO, tareaRepo = repo)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.save(TareaDraft(titulo = "Intento"))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(TareaError.NoAutorizada, state.error)
        assertNull(state.inicial)
        assertTrue(repo.updated.isEmpty())
    }

    @Test
    fun sinSesion_noAutorizado() = runTest {
        val viewModel = crearViewModel(user = null)

        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(TareaError.NoAutorizada, viewModel.uiState.value.error)
    }
}
