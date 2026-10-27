package com.unaj.appmoviles.features.home.agenda

import com.unaj.appmoviles.features.home.FakeAuthRepository
import com.unaj.appmoviles.features.home.FakeObraRepository
import com.unaj.appmoviles.features.home.USUARIO
import com.unaj.appmoviles.features.home.obraDe
import com.unaj.appmoviles.obratrack.core.domain.error.ObraError
import com.unaj.appmoviles.obratrack.core.domain.usecase.auth.ObserveSessionUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.ObserveAgendaVisitasUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.ScheduleVisitaUseCase
import kotlinx.coroutines.CompletableDeferred
import com.unaj.appmoviles.obratrack.core.domain.model.CategoriaAgendaVisita
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
import java.time.ZoneId
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

    private fun crearViewModel(obraRepo: FakeObraRepository, reloj: Clock = clock) = AgendaViewModel(
        observeSessionUseCase = ObserveSessionUseCase(FakeAuthRepository(USUARIO)),
        observeAgendaVisitasUseCase = ObserveAgendaVisitasUseCase(obraRepo, reloj),
        scheduleVisitaUseCase = ScheduleVisitaUseCase(obraRepo),
        clock = reloj
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

    private val medianocheDelDia = Instant.parse("2026-10-22T00:00:00Z").toEpochMilli()

    private class RelojMutable(var ahora: Instant) : Clock() {
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId): Clock = this
        override fun instant(): Instant = ahora
    }

    @Test
    fun alLlegarLaHora_laVisitaPasaAAtrasadaAlRefrescar_sinCambiarLasObras() = runTest {
        val reloj = RelojMutable(Instant.parse("2026-10-22T20:00:00Z"))
        val obra = obraDe().copy(proximaVisita = medianocheDelDia, horaVisita = 20 * 60 + 30)
        val viewModel = crearViewModel(FakeObraRepository(listOf(obra)), reloj)
        suscribir(viewModel)
        assertEquals(CategoriaAgendaVisita.HOY, viewModel.uiState.value.items.single().categoria)

        reloj.ahora = Instant.parse("2026-10-22T20:45:00Z")
        viewModel.refrescarHora()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(CategoriaAgendaVisita.URGENTE_VENCIDA, state.items.single().categoria)
        assertFalse(state.isLoading)
    }

    @Test
    fun reprogramar_conservaLaHoraElegida_yElDialogoSigueAbiertoMientrasGuarda() = runTest {
        val obra = obraDe().copy(proximaVisita = medianocheDelDia, horaVisita = 600)
        val repo = FakeObraRepository(listOf(obra)).apply { updateGate = CompletableDeferred() }
        val viewModel = crearViewModel(repo)
        suscribir(viewModel)
        viewModel.onIniciarReprogramacion(obra)
        testDispatcher.scheduler.advanceUntilIdle()

        val nueva = medianocheDelDia + 86_400_000L
        viewModel.onConfirmarReprogramacion(obra, nueva, 900)
        viewModel.onConfirmarReprogramacion(obra, nueva, 900)
        testDispatcher.scheduler.advanceUntilIdle()

        val guardando = viewModel.uiState.value
        assertTrue(guardando.isUpdating)
        assertEquals(obra, guardando.obraParaReprogramar)
        assertEquals(1, repo.actualizaciones.size)

        repo.updateGate!!.complete(Unit)
        testDispatcher.scheduler.advanceUntilIdle()

        val listo = viewModel.uiState.value
        assertFalse(listo.isUpdating)
        assertNull(listo.obraParaReprogramar)
        assertEquals(nueva, repo.actualizaciones.single().proximaVisita)
        assertEquals(900, repo.actualizaciones.single().horaVisita)
        assertEquals(FeedbackAgenda.VisitaProgramada(obra.nombre), listo.mensajeFeedback)
    }

    @Test
    fun reprogramar_sinHora_guardaLaFechaYBorraLaHora() = runTest {
        val obra = obraDe().copy(proximaVisita = medianocheDelDia, horaVisita = 600)
        val repo = FakeObraRepository(listOf(obra))
        val viewModel = crearViewModel(repo)
        suscribir(viewModel)

        val nueva = medianocheDelDia + 86_400_000L
        viewModel.onConfirmarReprogramacion(obra, nueva, null)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(nueva, repo.actualizaciones.single().proximaVisita)
        assertNull(repo.actualizaciones.single().horaVisita)
        assertEquals(FeedbackAgenda.VisitaProgramada(obra.nombre), viewModel.uiState.value.mensajeFeedback)
    }

    @Test
    fun quitarLaFecha_borraFechaYHora_yAvisaQueSeCancelo() = runTest {
        val obra = obraDe().copy(proximaVisita = medianocheDelDia, horaVisita = 600)
        val repo = FakeObraRepository(listOf(obra))
        val viewModel = crearViewModel(repo)
        suscribir(viewModel)

        viewModel.onConfirmarReprogramacion(obra, null, 600)
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(repo.actualizaciones.single().proximaVisita)
        assertNull(repo.actualizaciones.single().horaVisita)
        assertEquals(FeedbackAgenda.VisitaCancelada(obra.nombre), viewModel.uiState.value.mensajeFeedback)
    }

    @Test
    fun hoyMillis_enArgentinaALas22_esElDiaLocalYNoElSiguienteEnUtc() = runTest {
        // 22:00 del 22/10 en Buenos Aires = 01:00Z del 23/10.
        val reloj = Clock.fixed(Instant.parse("2026-10-23T01:00:00Z"), ZoneId.of("America/Argentina/Buenos_Aires"))
        val viewModel = crearViewModel(FakeObraRepository(listOf(obraDe())), reloj)

        suscribir(viewModel)

        assertEquals(Instant.parse("2026-10-22T00:00:00Z").toEpochMilli(), viewModel.uiState.value.hoyMillis)
    }
}
