package com.unaj.appmoviles.features.obras.relevamiento.detail

import com.unaj.appmoviles.features.obras.tareas.DUENO
import com.unaj.appmoviles.features.obras.tareas.FakeAuthRepository
import com.unaj.appmoviles.features.obras.tareas.FakeObraRepository
import com.unaj.appmoviles.features.obras.tareas.FakeRelevamientoRepository
import com.unaj.appmoviles.features.obras.tareas.OBRA_ID
import com.unaj.appmoviles.features.obras.tareas.obraDe
import com.unaj.appmoviles.obratrack.core.domain.error.RelevamientoError
import com.unaj.appmoviles.obratrack.core.domain.model.Relevamiento
import com.unaj.appmoviles.obratrack.core.domain.usecase.auth.ObserveSessionUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.ObserveObraPublicaUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.ObserveObraUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.relevamientos.DeleteRelevamientoUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.relevamientos.ObserveRelevamientosDeObraUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import com.unaj.appmoviles.features.obras.export.EstadoPdf
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RelevamientoDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

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

    private fun crearViewModel(
        relevamientoRepo: FakeRelevamientoRepository,
        obraRepo: FakeObraRepository = FakeObraRepository(obraDe(DUENO.id))
    ) = RelevamientoDetailViewModel(
        obraId = OBRA_ID,
        relevamientoId = "r1",
        observeSession = ObserveSessionUseCase(FakeAuthRepository(DUENO)),
        observeObra = ObserveObraUseCase(obraRepo),
        observeObraPublica = ObserveObraPublicaUseCase(obraRepo),
        observeRelevamientosDeObra = ObserveRelevamientosDeObraUseCase(relevamientoRepo),
        deleteRelevamientoUseCase = DeleteRelevamientoUseCase(relevamientoRepo)
    )

    @Test
    fun muestraElRelevamiento_sinError() = runTest {
        val viewModel = crearViewModel(FakeRelevamientoRepository(listOf(relevamiento)))

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(relevamiento, state.relevamiento)
        assertTrue(state.isEditable)
        assertNull(state.error)
        assertFalse(state.isLoading)
    }

    @Test
    fun relevamientoInexistente_esNoEncontrado() = runTest {
        val viewModel = crearViewModel(FakeRelevamientoRepository())

        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(RelevamientoError.NoEncontrado, viewModel.uiState.value.error)
    }

    @Test
    fun errorAlLeer_muestraErrorYReintentarVuelveAMostrarElRelevamiento() = runTest {
        val repo = FakeRelevamientoRepository(listOf(relevamiento)).apply {
            observeError = IllegalStateException("UNAVAILABLE")
        }
        val viewModel = crearViewModel(repo)
        testDispatcher.scheduler.advanceUntilIdle()

        val conError = viewModel.uiState.value
        assertEquals(RelevamientoError.Desconocido, conError.error)
        assertNull(conError.relevamiento)
        assertFalse(conError.isLoading)

        repo.observeError = null
        viewModel.reintentar()
        assertTrue(viewModel.uiState.value.isLoading)
        testDispatcher.scheduler.advanceUntilIdle()

        val recuperado = viewModel.uiState.value
        assertNull(recuperado.error)
        assertEquals(relevamiento, recuperado.relevamiento)
        assertFalse(recuperado.isLoading)
    }

    @Test
    fun pdf_siFallaQuedaEnErrorYSePuedeVolverAIntentar() = runTest {
        val viewModel = crearViewModel(FakeRelevamientoRepository(listOf(relevamiento)))
        assertEquals(EstadoPdf.Inactivo, viewModel.pdf.value)

        viewModel.generarPdf { null }
        assertEquals(EstadoPdf.Generando, viewModel.pdf.value)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(EstadoPdf.Error, viewModel.pdf.value)

        viewModel.pdfConsumido()
        assertEquals(EstadoPdf.Inactivo, viewModel.pdf.value)
    }

    @Test
    fun pdf_dosToquesSeguidosGeneranUnSolo() = runTest {
        val viewModel = crearViewModel(FakeRelevamientoRepository(listOf(relevamiento)))
        var llamadas = 0

        viewModel.generarPdf { llamadas++; null }
        viewModel.generarPdf { llamadas++; null }
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, llamadas)
    }
}
