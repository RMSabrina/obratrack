package com.unaj.appmoviles.features.obras.tareas.detail

import com.unaj.appmoviles.features.obras.tareas.DUENO
import com.unaj.appmoviles.features.obras.tareas.FakeAuthRepository
import com.unaj.appmoviles.features.obras.tareas.FakeObraRepository
import com.unaj.appmoviles.features.obras.tareas.FakeRelevamientoRepository
import com.unaj.appmoviles.features.obras.tareas.FakeTareaRepository
import com.unaj.appmoviles.features.obras.tareas.OBRA_ID
import com.unaj.appmoviles.features.obras.tareas.OTRO_USUARIO
import com.unaj.appmoviles.features.obras.tareas.obraDe
import com.unaj.appmoviles.obratrack.core.domain.error.TareaError
import com.unaj.appmoviles.obratrack.core.domain.model.CategoriaTarea
import com.unaj.appmoviles.obratrack.core.domain.model.EstadoTarea
import com.unaj.appmoviles.obratrack.core.domain.model.Relevamiento
import com.unaj.appmoviles.obratrack.core.domain.model.RelevamientosVinculados
import com.unaj.appmoviles.obratrack.core.domain.model.Tarea
import com.unaj.appmoviles.obratrack.core.domain.model.TareaDraft
import com.unaj.appmoviles.obratrack.core.domain.model.User
import com.unaj.appmoviles.obratrack.core.domain.usecase.auth.ObserveSessionUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.ObserveObraUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.relevamientos.ObserveRelevamientosDeTareaUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.tareas.DeleteTareaUseCase
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
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class TareaDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    // 22:00 del 21/10 en Buenos Aires (01:00 UTC del 22): "hoy" es el 21.
    private val clock = Clock.fixed(
        Instant.parse("2026-10-22T01:00:00Z"),
        ZoneId.of("America/Argentina/Buenos_Aires")
    )

    // Todos los campos cargados, para comprobar que completar no pierde ninguno.
    private val tarea = Tarea(
        id = "t1",
        obraId = OBRA_ID,
        userId = DUENO.id,
        titulo = "Instalar tablero",
        descripcion = "Tablero principal con disyuntor",
        categoria = CategoriaTarea.ELECTRICIDAD,
        fechaInicio = LocalDate.of(2026, 10, 1),
        fechaFin = LocalDate.of(2026, 10, 20),
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
        tareaId: String = tarea.id,
        user: User? = DUENO,
        repo: FakeTareaRepository = FakeTareaRepository(listOf(tarea)),
        relevamientoRepo: FakeRelevamientoRepository = FakeRelevamientoRepository()
    ) = TareaDetailViewModel(
        obraId = OBRA_ID,
        tareaId = tareaId,
        observeSession = ObserveSessionUseCase(FakeAuthRepository(user)),
        observeObra = ObserveObraUseCase(FakeObraRepository(obraDe(DUENO.id, publica = true))),
        observeTarea = ObserveTareaUseCase(repo),
        observeRelevamientosDeTarea = ObserveRelevamientosDeTareaUseCase(relevamientoRepo),
        updateTarea = UpdateTareaUseCase(repo),
        deleteTarea = DeleteTareaUseCase(repo),
        clock = clock
    )

    @Test
    fun dueno_veLaTareaYSiEstaAtrasada() = runTest {
        val viewModel = crearViewModel()

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals(tarea, state.tarea)
        assertTrue(state.atrasada)
    }

    @Test
    fun completar_soloCambiaElEstadoYConservaElRestoDeLosCampos() = runTest {
        val repo = FakeTareaRepository(listOf(tarea))
        val viewModel = crearViewModel(repo = repo)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.completar()
        testDispatcher.scheduler.advanceUntilIdle()

        val (userId, tareaId, draft) = repo.updated.single()
        assertEquals(DUENO.id, userId)
        assertEquals(tarea.id, tareaId)
        assertEquals(
            TareaDraft(
                titulo = tarea.titulo,
                descripcion = tarea.descripcion,
                categoria = tarea.categoria,
                fechaInicio = tarea.fechaInicio,
                fechaFin = tarea.fechaFin,
                estado = EstadoTarea.COMPLETADA
            ),
            draft
        )
        val state = viewModel.uiState.value
        assertFalse(state.isCompleting)
        assertEquals(tarea.copy(estado = EstadoTarea.COMPLETADA), state.tarea)
        // Completada ya no está atrasada.
        assertFalse(state.atrasada)
    }

    @Test
    fun completar_conError_muestraElErrorYPermiteReintentar() = runTest {
        val repo = FakeTareaRepository(listOf(tarea)).apply { writeError = TareaError.Red }
        val viewModel = crearViewModel(repo = repo)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.completar()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(TareaError.Red, state.actionError)
        assertFalse(state.isCompleting)
        assertEquals(EstadoTarea.EN_CURSO, state.tarea?.estado)

        viewModel.onActionErrorShown()
        assertNull(viewModel.uiState.value.actionError)
    }

    @Test
    fun completar_tareaYaCompletada_noLaVuelveAGuardar() = runTest {
        val completada = tarea.copy(estado = EstadoTarea.COMPLETADA)
        val repo = FakeTareaRepository(listOf(completada))
        val viewModel = crearViewModel(repo = repo)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.completar()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(repo.updated.isEmpty())
    }

    @Test
    fun eliminar_conExito_terminaEliminada() = runTest {
        val repo = FakeTareaRepository(listOf(tarea))
        val viewModel = crearViewModel(repo = repo)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.eliminar()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isDeleted)
        assertEquals(OBRA_ID to tarea.id, repo.deleted.single())
    }

    @Test
    fun eliminar_conError_muestraElErrorYMantieneLaTarea() = runTest {
        val repo = FakeTareaRepository(listOf(tarea)).apply { deleteError = TareaError.Red }
        val viewModel = crearViewModel(repo = repo)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.eliminar()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(TareaError.Red, state.deleteError)
        assertFalse(state.isDeleting)
        assertFalse(state.isDeleted)
        assertEquals(tarea, state.tarea)
    }

    @Test
    fun eliminar_timeoutConLaTareaYaBorradaDelCache_terminaEliminada() = runTest {
        val repo = FakeTareaRepository(listOf(tarea)).apply {
            deleteError = TareaError.Timeout
            deleteRemovesOnError = true
        }
        val viewModel = crearViewModel(repo = repo)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.eliminar()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isDeleted)
    }

    @Test
    fun eliminar_timeoutConLaTareaTodaviaPresente_muestraError() = runTest {
        val repo = FakeTareaRepository(listOf(tarea)).apply { deleteError = TareaError.Timeout }
        val viewModel = crearViewModel(repo = repo)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.eliminar()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(TareaError.Timeout, state.deleteError)
        assertFalse(state.isDeleted)
    }

    @Test
    fun tareaInexistente_noEncontrada() = runTest {
        val viewModel = crearViewModel(tareaId = "no-existe")

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(TareaError.NoEncontrada, state.error)
        assertNull(state.tarea)
    }

    @Test
    fun noDueno_obraPublica_noAutorizadoYNoPuedeCompletarNiEliminar() = runTest {
        val repo = FakeTareaRepository(listOf(tarea))
        val viewModel = crearViewModel(user = OTRO_USUARIO, repo = repo)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.completar()
        viewModel.eliminar()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(TareaError.NoAutorizada, state.error)
        assertNull(state.tarea)
        assertTrue(repo.updated.isEmpty())
        assertTrue(repo.deleted.isEmpty())
    }

    private fun relevamiento(id: String, tareaId: String?) = Relevamiento(
        id = id,
        obraId = OBRA_ID,
        userId = DUENO.id,
        fecha = 0L,
        nota = "Nota $id",
        tareaId = tareaId
    )

    @Test
    fun dueno_veSoloLosRelevamientosVinculadosALaTarea() = runTest {
        val vinculado = relevamiento("r1", tareaId = tarea.id)
        val relevamientoRepo = FakeRelevamientoRepository(
            listOf(
                vinculado,
                relevamiento("r2", tareaId = "otra-tarea"),
                // Anterior a la Etapa 4: sin tareaId, no aparece en ninguna tarea.
                relevamiento("r3", tareaId = null)
            )
        )
        val viewModel = crearViewModel(relevamientoRepo = relevamientoRepo)

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf(vinculado), state.vinculados)
        assertFalse(state.vinculadosError)
    }

    @Test
    fun errorAlLeerVinculados_avisaSinOcultarLaTareaYNoPermiteEliminar() = runTest {
        val repo = FakeTareaRepository(listOf(tarea))
        val relevamientoRepo = FakeRelevamientoRepository().apply {
            observeByTareaError = IllegalStateException("PERMISSION_DENIED")
        }
        val viewModel = crearViewModel(repo = repo, relevamientoRepo = relevamientoRepo)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.eliminar(RelevamientosVinculados.DESVINCULAR)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.vinculadosError)
        assertEquals(tarea, state.tarea)
        assertTrue(repo.deleted.isEmpty())
    }

    @Test
    fun eliminar_pasaLaOpcionElegidaParaLosVinculados() = runTest {
        val repo = FakeTareaRepository(listOf(tarea))
        val relevamientoRepo = FakeRelevamientoRepository(listOf(relevamiento("r1", tareaId = tarea.id)))
        val viewModel = crearViewModel(repo = repo, relevamientoRepo = relevamientoRepo)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.eliminar(RelevamientosVinculados.ELIMINAR)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isDeleted)
        assertEquals(RelevamientosVinculados.ELIMINAR, repo.deletedVinculados.single())
    }

    @Test
    fun eliminar_sinElegir_desvinculaPorDefecto() = runTest {
        val repo = FakeTareaRepository(listOf(tarea))
        val viewModel = crearViewModel(repo = repo)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.eliminar()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(RelevamientosVinculados.DESVINCULAR, repo.deletedVinculados.single())
    }

    @Test
    fun noDueno_noConsultaLosVinculados() = runTest {
        val relevamientoRepo = FakeRelevamientoRepository(listOf(relevamiento("r1", tareaId = tarea.id)))
        val viewModel = crearViewModel(user = OTRO_USUARIO, relevamientoRepo = relevamientoRepo)

        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.vinculados.isEmpty())
        assertEquals(0, relevamientoRepo.observeByTareaCalls)
    }

    @Test
    fun sinSesion_noAutorizado() = runTest {
        val viewModel = crearViewModel(user = null)

        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(TareaError.NoAutorizada, viewModel.uiState.value.error)
    }
}
