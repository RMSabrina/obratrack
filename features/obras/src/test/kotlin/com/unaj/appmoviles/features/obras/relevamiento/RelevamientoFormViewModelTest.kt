package com.unaj.appmoviles.features.obras.relevamiento

import com.unaj.appmoviles.features.obras.tareas.DUENO
import com.unaj.appmoviles.features.obras.tareas.FakeAuthRepository
import com.unaj.appmoviles.features.obras.tareas.FakeObraRepository
import com.unaj.appmoviles.features.obras.tareas.FakeRelevamientoRepository
import com.unaj.appmoviles.features.obras.tareas.FakeTareaRepository
import com.unaj.appmoviles.features.obras.tareas.OBRA_ID
import com.unaj.appmoviles.features.obras.tareas.OTRO_USUARIO
import com.unaj.appmoviles.features.obras.tareas.obraDe
import com.unaj.appmoviles.obratrack.core.domain.model.EstadoTarea
import com.unaj.appmoviles.obratrack.core.domain.model.RelevamientoDraft
import com.unaj.appmoviles.obratrack.core.domain.model.Tarea
import com.unaj.appmoviles.obratrack.core.domain.model.User
import com.unaj.appmoviles.obratrack.core.domain.usecase.auth.ObserveSessionUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.ObserveObraUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.relevamientos.CreateRelevamientoUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.relevamientos.UpdateRelevamientoUseCase
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
import com.unaj.appmoviles.obratrack.core.domain.error.RelevamientoError
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class RelevamientoFormViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    // 22:00 del 21/10 en Buenos Aires (01:00 UTC del 22): "hoy" es el 21.
    private val clock = Clock.fixed(
        Instant.parse("2026-10-22T01:00:00Z"),
        ZoneId.of("America/Argentina/Buenos_Aires")
    )

    private fun tarea(
        id: String,
        titulo: String,
        inicio: LocalDate? = null,
        estado: EstadoTarea = EstadoTarea.PENDIENTE
    ) = Tarea(
        id = id,
        obraId = OBRA_ID,
        userId = DUENO.id,
        titulo = titulo,
        fechaInicio = inicio,
        fechaFin = inicio,
        estado = estado
    )

    private val sinFechas = tarea("t-sin", "Sin fechas")
    private val pintura = tarea("t-pint", "Pintura", LocalDate.of(2026, 10, 10), EstadoTarea.COMPLETADA)
    private val cimientos = tarea("t-cim", "Cimientos", LocalDate.of(2026, 10, 1))

    private val draft = RelevamientoDraft(fecha = 1_700_000_000_000L, nota = "Avance")

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun crearViewModel(
        user: User? = DUENO,
        relevamientoId: String? = null,
        tareaIdInicial: String? = null,
        tareaRepo: FakeTareaRepository = FakeTareaRepository(listOf(sinFechas, pintura, cimientos)),
        relevamientoRepo: FakeRelevamientoRepository = FakeRelevamientoRepository()
    ) = RelevamientoFormViewModel(
        observeSession = ObserveSessionUseCase(FakeAuthRepository(user)),
        createRelevamiento = CreateRelevamientoUseCase(relevamientoRepo),
        updateRelevamiento = UpdateRelevamientoUseCase(relevamientoRepo),
        observeObra = ObserveObraUseCase(FakeObraRepository(obraDe(DUENO.id, publica = true))),
        observeTareas = ObserveTareasUseCase(tareaRepo),
        obraId = OBRA_ID,
        relevamientoId = relevamientoId,
        tareaIdInicial = tareaIdInicial,
        clock = clock
    )

    @Test
    fun hoyMillisUtc_esLaMedianocheUtcDelDiaLocal_aunqueEnUtcYaSeaManana() {
        val viewModel = crearViewModel()

        assertEquals(1_792_540_800_000L, viewModel.hoyMillisUtc) // 2026-10-21T00:00:00Z
    }

    @Test
    fun dueno_veTodasLasTareasEnElOrdenDelListado_conLasCompletadasMarcadas() = runTest {
        val viewModel = crearViewModel()

        testDispatcher.scheduler.advanceUntilIdle()

        val selector = viewModel.selector.value
        assertTrue(selector.disponible)
        assertEquals(
            listOf(
                OpcionTarea("t-cim", "Cimientos", completada = false),
                OpcionTarea("t-pint", "Pintura", completada = true),
                OpcionTarea("t-sin", "Sin fechas", completada = false)
            ),
            selector.opciones
        )
        assertNull(selector.seleccionada)
    }

    @Test
    fun crear_conTareaElegida_guardaElVinculoYNoTocaLaTarea() = runTest {
        val tareaRepo = FakeTareaRepository(listOf(sinFechas, pintura, cimientos))
        val relevamientoRepo = FakeRelevamientoRepository()
        val viewModel = crearViewModel(tareaRepo = tareaRepo, relevamientoRepo = relevamientoRepo)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onTareaSeleccionada("t-cim")
        viewModel.save(draft, null)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("t-cim", relevamientoRepo.created.single().tareaId)
        // Crear un relevamiento nunca cambia el estado de una tarea.
        assertTrue(tareaRepo.updated.isEmpty())
        assertEquals(RelevamientoFormUiState.Saved, viewModel.uiState.value)
    }

    @Test
    fun editar_elegirNinguna_desvincula() = runTest {
        val relevamientoRepo = FakeRelevamientoRepository()
        val viewModel = crearViewModel(
            relevamientoId = "r1",
            tareaIdInicial = "t-cim",
            relevamientoRepo = relevamientoRepo
        )
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals("t-cim", viewModel.selector.value.seleccionada)

        viewModel.onTareaSeleccionada(null)
        viewModel.save(draft, null)
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(relevamientoRepo.updated.single().second.tareaId)
    }

    @Test
    fun tareaInexistente_seVeSinVinculo_yAlGuardarSeLimpiaElCampo() = runTest {
        val relevamientoRepo = FakeRelevamientoRepository()
        val viewModel = crearViewModel(
            relevamientoId = "r1",
            tareaIdInicial = "tarea-borrada",
            relevamientoRepo = relevamientoRepo
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // En pantalla: "Ninguna".
        assertNull(viewModel.selector.value.seleccionada)

        viewModel.save(draft, null)
        testDispatcher.scheduler.advanceUntilIdle()

        // tareaId null llega al data source, que escribe FieldValue.delete() (ver valorTareaIdParaMerge).
        assertNull(relevamientoRepo.updated.single().second.tareaId)
    }

    @Test
    fun errorAlLeerTareas_conservaElVinculoAlGuardar() = runTest {
        val tareaRepo = FakeTareaRepository().apply { observeError = IllegalStateException("UNAVAILABLE") }
        val relevamientoRepo = FakeRelevamientoRepository()
        val viewModel = crearViewModel(
            relevamientoId = "r1",
            tareaIdInicial = "t-cim",
            tareaRepo = tareaRepo,
            relevamientoRepo = relevamientoRepo
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val selector = viewModel.selector.value
        assertTrue(selector.error)
        assertFalse(selector.disponible)

        viewModel.save(draft, null)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("t-cim", relevamientoRepo.updated.single().second.tareaId)
    }

    @Test
    fun seleccionarUnaTareaQueNoEsDeLaObra_seIgnora() = runTest {
        val viewModel = crearViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onTareaSeleccionada("de-otra-obra")

        assertNull(viewModel.selector.value.seleccionada)
    }

    @Test
    fun noDueno_noVeSelectorYNoConsultaTareas() = runTest {
        val tareaRepo = FakeTareaRepository(listOf(cimientos))
        val viewModel = crearViewModel(user = OTRO_USUARIO, tareaRepo = tareaRepo)

        testDispatcher.scheduler.advanceUntilIdle()

        val selector = viewModel.selector.value
        assertFalse(selector.disponible)
        assertFalse(selector.error)
        assertTrue(selector.opciones.isEmpty())
        assertEquals(0, tareaRepo.observeByObraCalls)
    }

    @Test
    fun sinSesion_noConsultaTareas() = runTest {
        val tareaRepo = FakeTareaRepository(listOf(cimientos))
        val viewModel = crearViewModel(user = null, tareaRepo = tareaRepo)

        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.selector.value.disponible)
        assertEquals(0, tareaRepo.observeByObraCalls)
    }

    @Test
    fun foto_dePocosMb_seGuardaPorqueSeAchicaAlSubir() = runTest {
        val relevamientoRepo = FakeRelevamientoRepository()
        val viewModel = crearViewModel(relevamientoRepo = relevamientoRepo)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.save(draft, ByteArray(8 * 1024 * 1024))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, relevamientoRepo.created.size)
        assertEquals(RelevamientoFormUiState.Saved, viewModel.uiState.value)
    }

    @Test
    fun foto_enorme_noSeIntentaSubirYMuestraElError() = runTest {
        val relevamientoRepo = FakeRelevamientoRepository()
        val viewModel = crearViewModel(relevamientoRepo = relevamientoRepo)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.save(draft, ByteArray(RelevamientoFormViewModel.MAX_FOTO_ORIGINAL_BYTES + 1))
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(relevamientoRepo.created.isEmpty())
        assertEquals(RelevamientoFormUiState.Error(RelevamientoError.ImagenMuyGrande), viewModel.uiState.value)
    }
}
