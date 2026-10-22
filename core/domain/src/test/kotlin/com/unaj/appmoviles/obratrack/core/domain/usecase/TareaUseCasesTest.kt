package com.unaj.appmoviles.obratrack.core.domain.usecase

import com.unaj.appmoviles.obratrack.core.domain.error.AppResult
import com.unaj.appmoviles.obratrack.core.domain.error.TareaError
import com.unaj.appmoviles.obratrack.core.domain.model.Tarea
import com.unaj.appmoviles.obratrack.core.domain.model.TareaDraft
import com.unaj.appmoviles.obratrack.core.domain.repository.TareaRepository
import com.unaj.appmoviles.obratrack.core.domain.usecase.tareas.CreateTareaUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.tareas.UpdateTareaUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class TareaUseCasesTest {

    private val inicio = LocalDate.of(2026, 10, 1)
    private val fin = LocalDate.of(2026, 10, 10)

    @Test
    fun create_rechazaTituloVacio_yNoLlamaAlRepositorio() = runBlocking {
        val repository = FakeTareaRepository()
        val result = CreateTareaUseCase(repository)("uid", "obra-1", draft(titulo = "   "))

        assertEquals(TareaError.TituloVacio, (result as AppResult.Error).error)
        assertEquals(0, repository.createCalls)
    }

    @Test
    fun create_rechazaSoloFechaInicio() = runBlocking {
        val repository = FakeTareaRepository()
        val result = CreateTareaUseCase(repository)("uid", "obra-1", draft(fechaInicio = inicio))

        assertEquals(TareaError.FechasIncompletas, (result as AppResult.Error).error)
        assertEquals(0, repository.createCalls)
    }

    @Test
    fun create_rechazaSoloFechaFin() = runBlocking {
        val repository = FakeTareaRepository()
        val result = CreateTareaUseCase(repository)("uid", "obra-1", draft(fechaFin = fin))

        assertEquals(TareaError.FechasIncompletas, (result as AppResult.Error).error)
        assertEquals(0, repository.createCalls)
    }

    @Test
    fun create_rechazaFinAnteriorAInicio() = runBlocking {
        val repository = FakeTareaRepository()
        val result = CreateTareaUseCase(repository)(
            "uid", "obra-1", draft(fechaInicio = fin, fechaFin = inicio)
        )

        assertEquals(TareaError.FechaFinAnterior, (result as AppResult.Error).error)
        assertEquals(0, repository.createCalls)
    }

    @Test
    fun create_aceptaFinIgualAInicio() = runBlocking {
        val repository = FakeTareaRepository()
        val result = CreateTareaUseCase(repository)(
            "uid", "obra-1", draft(fechaInicio = inicio, fechaFin = inicio)
        )

        assertTrue(result is AppResult.Success)
        assertEquals(1, repository.createCalls)
    }

    @Test
    fun create_aceptaSinFechas() = runBlocking {
        val repository = FakeTareaRepository()
        val result = CreateTareaUseCase(repository)("uid", "obra-1", draft())

        assertTrue(result is AppResult.Success)
        assertEquals(1, repository.createCalls)
    }

    @Test
    fun create_recortaTextos_yDelegaLaObra() = runBlocking {
        val repository = FakeTareaRepository()
        val result = CreateTareaUseCase(repository)(
            "uid", "obra-1", draft(titulo = "  Instalar tablero  ", descripcion = "  Planta baja ")
        )

        assertTrue(result is AppResult.Success)
        assertEquals("obra-1", repository.lastObraId)
        assertEquals("Instalar tablero", repository.lastDraft?.titulo)
        assertEquals("Planta baja", repository.lastDraft?.descripcion)
    }

    @Test
    fun update_rechazaTituloVacio_yNoLlamaAlRepositorio() = runBlocking {
        val repository = FakeTareaRepository()
        val result = UpdateTareaUseCase(repository)("uid", "obra-1", "tarea-1", draft(titulo = ""))

        assertEquals(TareaError.TituloVacio, (result as AppResult.Error).error)
        assertEquals(0, repository.updateCalls)
    }

    @Test
    fun update_rechazaFechasIncompletas_yNoLlamaAlRepositorio() = runBlocking {
        val repository = FakeTareaRepository()
        val result = UpdateTareaUseCase(repository)(
            "uid", "obra-1", "tarea-1", draft(fechaFin = fin)
        )

        assertEquals(TareaError.FechasIncompletas, (result as AppResult.Error).error)
        assertEquals(0, repository.updateCalls)
    }

    @Test
    fun update_valido_delegaAlRepositorio() = runBlocking {
        val repository = FakeTareaRepository()
        val result = UpdateTareaUseCase(repository)(
            "uid", "obra-1", "tarea-1", draft(fechaInicio = inicio, fechaFin = fin)
        )

        assertEquals("tarea-1", (result as AppResult.Success).data.id)
        assertEquals(1, repository.updateCalls)
    }

    private fun draft(
        titulo: String = "Tarea",
        descripcion: String = "",
        fechaInicio: LocalDate? = null,
        fechaFin: LocalDate? = null
    ) = TareaDraft(
        titulo = titulo,
        descripcion = descripcion,
        fechaInicio = fechaInicio,
        fechaFin = fechaFin
    )
}

private class FakeTareaRepository : TareaRepository {
    var createCalls: Int = 0
    var updateCalls: Int = 0
    var lastObraId: String? = null
    var lastDraft: TareaDraft? = null

    override fun observeByObra(obraId: String): Flow<List<Tarea>> = emptyFlow()

    override fun observeOne(obraId: String, tareaId: String): Flow<Tarea?> = emptyFlow()

    override suspend fun create(userId: String, obraId: String, draft: TareaDraft): AppResult<Tarea> {
        createCalls += 1
        lastObraId = obraId
        lastDraft = draft
        return AppResult.Success(draft.toTarea(id = "nueva", obraId = obraId, userId = userId))
    }

    override suspend fun update(
        userId: String,
        obraId: String,
        tareaId: String,
        draft: TareaDraft
    ): AppResult<Tarea> {
        updateCalls += 1
        lastDraft = draft
        return AppResult.Success(draft.toTarea(id = tareaId, obraId = obraId, userId = userId))
    }

    override suspend fun delete(obraId: String, tareaId: String): AppResult<Unit> = AppResult.Success(Unit)
    override suspend fun deleteAllByObra(obraId: String): AppResult<Unit> = AppResult.Success(Unit)

    private fun TareaDraft.toTarea(id: String, obraId: String, userId: String) = Tarea(
        id = id,
        obraId = obraId,
        userId = userId,
        titulo = titulo,
        descripcion = descripcion,
        categoria = categoria,
        fechaInicio = fechaInicio,
        fechaFin = fechaFin,
        estado = estado
    )
}
