package com.unaj.appmoviles.obratrack.core.domain.usecase

import com.unaj.appmoviles.obratrack.core.domain.error.AppResult
import com.unaj.appmoviles.obratrack.core.domain.error.RelevamientoError
import com.unaj.appmoviles.obratrack.core.domain.model.Relevamiento
import com.unaj.appmoviles.obratrack.core.domain.model.RelevamientoDraft
import com.unaj.appmoviles.obratrack.core.domain.repository.RelevamientoRepository
import com.unaj.appmoviles.obratrack.core.domain.usecase.relevamientos.CreateRelevamientoUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.relevamientos.UpdateRelevamientoUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.relevamientos.SincronizarProximaVisitaObraUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RelevamientoUseCasesTest {

    @Test
    fun create_rechazaNotaVacia_yNoLlamaAlRepositorio() = runBlocking {
        val repository = FakeRelevamientoRepository()
        val result = CreateRelevamientoUseCase(repository)("uid", "obra-1", draft(nota = "  "))

        assertEquals(RelevamientoError.NotaVacia, (result as AppResult.Error).error)
        assertEquals(0, repository.createCalls)
    }

    @Test
    fun create_rechazaFechaInvalida() = runBlocking {
        val repository = FakeRelevamientoRepository()
        val result = CreateRelevamientoUseCase(repository)("uid", "obra-1", draft(fecha = 0L))

        assertEquals(RelevamientoError.FechaInvalida, (result as AppResult.Error).error)
        assertEquals(0, repository.createCalls)
    }

    @Test
    fun create_recortaLaNota_yDelegaLaObra() = runBlocking {
        val repository = FakeRelevamientoRepository()
        val result = CreateRelevamientoUseCase(repository)("uid", "obra-1", draft(nota = "  Avance de losa  "))

        assertTrue(result is AppResult.Success)
        assertEquals("obra-1", repository.lastObraId)
        assertEquals("Avance de losa", repository.lastDraft?.nota)
    }

    @Test
    fun update_rechazaNotaVacia_yNoLlamaAlRepositorio() = runBlocking {
        val repository = FakeRelevamientoRepository()
        val result = UpdateRelevamientoUseCase(repository, SincronizarProximaVisitaObraUseCase(repository, FakeBaseObraRepository()))("uid", "obra-1", "rel-1", draft(nota = ""))

        assertEquals(RelevamientoError.NotaVacia, (result as AppResult.Error).error)
        assertEquals(0, repository.updateCalls)
    }

    private fun draft(fecha: Long = 1_700_000_000_000L, nota: String = "Nota") =
        RelevamientoDraft(fecha = fecha, nota = nota)
}

private class FakeRelevamientoRepository : RelevamientoRepository {
    var createCalls: Int = 0
    var updateCalls: Int = 0
    var lastObraId: String? = null
    var lastDraft: RelevamientoDraft? = null
    override fun observeAll(userId: String): Flow<List<Relevamiento>> {
        TODO("Not yet implemented")
    }

    override fun observe(userId: String, obraId: String): Flow<List<Relevamiento>> = emptyFlow()

    override fun observeByObra(obraId: String): Flow<List<Relevamiento>> = emptyFlow()

    override fun observeByTarea(obraId: String, tareaId: String): Flow<List<Relevamiento>> = emptyFlow()

    override suspend fun create(
        userId: String,
        obraId: String,
        draft: RelevamientoDraft,
        photo: ByteArray?
    ): AppResult<Relevamiento> {
        createCalls += 1
        lastObraId = obraId
        lastDraft = draft
        return AppResult.Success(
            Relevamiento(id = "nuevo", obraId = obraId, userId = userId, fecha = draft.fecha, nota = draft.nota)
        )
    }

    override suspend fun update(
        userId: String,
        obraId: String,
        relevamientoId: String,
        draft: RelevamientoDraft,
        photo: ByteArray?
    ): AppResult<Relevamiento> {
        updateCalls += 1
        lastDraft = draft
        return AppResult.Success(
            Relevamiento(
                id = relevamientoId,
                obraId = obraId,
                userId = userId,
                fecha = draft.fecha,
                nota = draft.nota
            )
        )
    }
    override suspend fun getByObra(obraId: String): AppResult<List<Relevamiento>> = AppResult.Success(emptyList())

    override suspend fun delete(obraId: String, relevamientoId: String): AppResult<Unit> = AppResult.Success(Unit)
    override suspend fun deleteAllByObra(obraId: String): AppResult<Unit> = AppResult.Success(Unit)
}
