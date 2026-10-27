package com.unaj.appmoviles.obratrack.core.domain.usecase

import com.unaj.appmoviles.obratrack.core.domain.error.AppResult
import com.unaj.appmoviles.obratrack.core.domain.error.ObraError
import com.unaj.appmoviles.obratrack.core.domain.error.RelevamientoError
import com.unaj.appmoviles.obratrack.core.domain.error.TareaError
import com.unaj.appmoviles.obratrack.core.domain.model.Obra
import com.unaj.appmoviles.obratrack.core.domain.model.ObraDraft
import com.unaj.appmoviles.obratrack.core.domain.model.Relevamiento
import com.unaj.appmoviles.obratrack.core.domain.model.RelevamientosVinculados
import com.unaj.appmoviles.obratrack.core.domain.model.RelevamientoDraft
import com.unaj.appmoviles.obratrack.core.domain.model.Tarea
import com.unaj.appmoviles.obratrack.core.domain.model.TareaDraft
import com.unaj.appmoviles.obratrack.core.domain.repository.ObraRepository
import com.unaj.appmoviles.obratrack.core.domain.repository.RelevamientoRepository
import com.unaj.appmoviles.obratrack.core.domain.repository.TareaRepository
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.DeleteObraUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeleteObraUseCaseTest {

    @Test
    fun delete_existsReturnsFalse_returnsSuccessWithoutCascade() = runBlocking {
        val obraRepo = FakeObraRepo(existsResult = AppResult.Success(false))
        val relevamientoRepo = FakeRelevamientoRepo()
        val tareaRepo = FakeTareaRepo()
        val useCase = DeleteObraUseCase(obraRepo, relevamientoRepo, tareaRepo)

        val result = useCase("obra-1")

        assertTrue(result is AppResult.Success)
        assertEquals(1, obraRepo.existsCalls)
        assertEquals(0, tareaRepo.deleteAllCalls)
        assertEquals(0, relevamientoRepo.deleteAllCalls)
        assertEquals(0, obraRepo.deleteAllSuscriptoresCalls)
        assertEquals(0, obraRepo.deleteCalls)
    }

    @Test
    fun delete_cascadePermissionDenied_andExistsReturnsFalse_returnsSuccess() = runBlocking {
        var existsCount = 0
        val obraRepo = object : FakeObraRepo() {
            override suspend fun existsAndOwnedByCurrentUser(obraId: String): AppResult<Boolean> {
                existsCount++
                return if (existsCount == 1) AppResult.Success(true) else AppResult.Success(false)
            }
        }
        val relevamientoRepo = FakeRelevamientoRepo(deleteAllResult = AppResult.Error(RelevamientoError.NoAutorizado))
        val tareaRepo = FakeTareaRepo()
        val useCase = DeleteObraUseCase(obraRepo, relevamientoRepo, tareaRepo)

        val result = useCase("obra-1")

        assertTrue(result is AppResult.Success)
        assertEquals(2, existsCount)
        assertEquals(1, tareaRepo.deleteAllCalls)
        assertEquals(1, relevamientoRepo.deleteAllCalls)
        assertEquals(0, obraRepo.deleteAllSuscriptoresCalls)
        assertEquals(0, obraRepo.deleteCalls)
    }

    @Test
    fun delete_cascadePermissionDenied_andExistsReturnsTrue_returnsFalloParcial() = runBlocking {
        val obraRepo = FakeObraRepo(existsResult = AppResult.Success(true))
        val relevamientoRepo = FakeRelevamientoRepo(deleteAllResult = AppResult.Error(RelevamientoError.NoAutorizado))
        val tareaRepo = FakeTareaRepo()
        val useCase = DeleteObraUseCase(obraRepo, relevamientoRepo, tareaRepo)

        val result = useCase("obra-1")

        assertTrue(result is AppResult.Error)
        assertEquals(ObraError.FalloParcial, (result as AppResult.Error).error)
        assertEquals(2, obraRepo.existsCalls)
        assertEquals(1, tareaRepo.deleteAllCalls)
        assertEquals(1, relevamientoRepo.deleteAllCalls)
        assertEquals(0, obraRepo.deleteAllSuscriptoresCalls)
        assertEquals(0, obraRepo.deleteCalls)
    }

    @Test
    fun delete_existenceCheckFails_propagatesError() = runBlocking {
        val obraRepo = FakeObraRepo(existsResult = AppResult.Error(ObraError.Red))
        val relevamientoRepo = FakeRelevamientoRepo()
        val tareaRepo = FakeTareaRepo()
        val useCase = DeleteObraUseCase(obraRepo, relevamientoRepo, tareaRepo)

        val result = useCase("obra-1")

        assertTrue(result is AppResult.Error)
        assertEquals(ObraError.Red, (result as AppResult.Error).error)
        assertEquals(1, obraRepo.existsCalls)
        assertEquals(0, tareaRepo.deleteAllCalls)
        assertEquals(0, relevamientoRepo.deleteAllCalls)
        assertEquals(0, obraRepo.deleteAllSuscriptoresCalls)
        assertEquals(0, obraRepo.deleteCalls)
    }

    @Test
    fun delete_tareasFails_restDoesNotRun() = runBlocking {
        val obraRepo = FakeObraRepo(existsResult = AppResult.Success(true))
        val relevamientoRepo = FakeRelevamientoRepo()
        val tareaRepo = FakeTareaRepo(deleteAllResult = AppResult.Error(TareaError.Red))
        val useCase = DeleteObraUseCase(obraRepo, relevamientoRepo, tareaRepo)

        val result = useCase("obra-1")

        assertTrue(result is AppResult.Error)
        assertEquals(ObraError.FalloParcial, (result as AppResult.Error).error)
        assertEquals(1, obraRepo.existsCalls)
        assertEquals(1, tareaRepo.deleteAllCalls)
        assertEquals(0, relevamientoRepo.deleteAllCalls)
        assertEquals(0, obraRepo.deleteAllSuscriptoresCalls)
        assertEquals(0, obraRepo.deleteCalls)
    }

    @Test
    fun delete_tareasPermissionDenied_andExistsReturnsFalse_returnsSuccess() = runBlocking {
        var existsCount = 0
        val obraRepo = object : FakeObraRepo() {
            override suspend fun existsAndOwnedByCurrentUser(obraId: String): AppResult<Boolean> {
                existsCount++
                return if (existsCount == 1) AppResult.Success(true) else AppResult.Success(false)
            }
        }
        val relevamientoRepo = FakeRelevamientoRepo()
        val tareaRepo = FakeTareaRepo(deleteAllResult = AppResult.Error(TareaError.NoAutorizada))
        val useCase = DeleteObraUseCase(obraRepo, relevamientoRepo, tareaRepo)

        val result = useCase("obra-1")

        assertTrue(result is AppResult.Success)
        assertEquals(2, existsCount)
        assertEquals(1, tareaRepo.deleteAllCalls)
        assertEquals(0, relevamientoRepo.deleteAllCalls)
        assertEquals(0, obraRepo.deleteAllSuscriptoresCalls)
        assertEquals(0, obraRepo.deleteCalls)
    }

    @Test
    fun delete_tareasPermissionDenied_andExistsReturnsTrue_returnsFalloParcial() = runBlocking {
        val obraRepo = FakeObraRepo(existsResult = AppResult.Success(true))
        val relevamientoRepo = FakeRelevamientoRepo()
        val tareaRepo = FakeTareaRepo(deleteAllResult = AppResult.Error(TareaError.NoAutorizada))
        val useCase = DeleteObraUseCase(obraRepo, relevamientoRepo, tareaRepo)

        val result = useCase("obra-1")

        assertTrue(result is AppResult.Error)
        assertEquals(ObraError.FalloParcial, (result as AppResult.Error).error)
        assertEquals(2, obraRepo.existsCalls)
        assertEquals(1, tareaRepo.deleteAllCalls)
        assertEquals(0, relevamientoRepo.deleteAllCalls)
        assertEquals(0, obraRepo.deleteAllSuscriptoresCalls)
        assertEquals(0, obraRepo.deleteCalls)
    }

    @Test
    fun delete_step1Fails_steps2And3DoNotRun() = runBlocking {
        val obraRepo = FakeObraRepo(existsResult = AppResult.Success(true))
        val relevamientoRepo = FakeRelevamientoRepo(deleteAllResult = AppResult.Error(RelevamientoError.Red))
        val tareaRepo = FakeTareaRepo()
        val useCase = DeleteObraUseCase(obraRepo, relevamientoRepo, tareaRepo)

        val result = useCase("obra-1")

        assertTrue(result is AppResult.Error)
        assertEquals(ObraError.FalloParcial, (result as AppResult.Error).error)
        assertEquals(1, tareaRepo.deleteAllCalls)
        assertEquals(1, relevamientoRepo.deleteAllCalls)
        assertEquals(0, obraRepo.deleteAllSuscriptoresCalls)
        assertEquals(0, obraRepo.deleteCalls)
    }

    @Test
    fun delete_step2Fails_step3DoesNotRun() = runBlocking {
        val obraRepo = FakeObraRepo(
            existsResult = AppResult.Success(true),
            deleteAllSuscriptoresResult = AppResult.Error(ObraError.Red)
        )
        val relevamientoRepo = FakeRelevamientoRepo(deleteAllResult = AppResult.Success(Unit))
        val tareaRepo = FakeTareaRepo()
        val useCase = DeleteObraUseCase(obraRepo, relevamientoRepo, tareaRepo)

        val result = useCase("obra-1")

        assertTrue(result is AppResult.Error)
        assertEquals(ObraError.FalloParcial, (result as AppResult.Error).error)
        assertEquals(1, tareaRepo.deleteAllCalls)
        assertEquals(1, relevamientoRepo.deleteAllCalls)
        assertEquals(1, obraRepo.deleteAllSuscriptoresCalls)
        assertEquals(0, obraRepo.deleteCalls)
    }

    @Test
    fun delete_allStepsSucceed_returnsSuccess() = runBlocking {
        val obraRepo = FakeObraRepo(existsResult = AppResult.Success(true))
        val relevamientoRepo = FakeRelevamientoRepo(deleteAllResult = AppResult.Success(Unit))
        val tareaRepo = FakeTareaRepo()
        val useCase = DeleteObraUseCase(obraRepo, relevamientoRepo, tareaRepo)

        val result = useCase("obra-1")

        assertTrue(result is AppResult.Success)
        assertEquals(1, tareaRepo.deleteAllCalls)
        assertEquals(1, relevamientoRepo.deleteAllCalls)
        assertEquals(1, obraRepo.deleteAllSuscriptoresCalls)
        assertEquals(1, obraRepo.deleteCalls)
    }
}

open class FakeObraRepo(
    var existsResult: AppResult<Boolean> = AppResult.Success(true),
    var deleteResult: AppResult<Unit> = AppResult.Success(Unit),
    var deleteAllSuscriptoresResult: AppResult<Unit> = AppResult.Success(Unit)
) : ObraRepository {
    var existsCalls = 0
    var deleteCalls = 0
    var deleteAllSuscriptoresCalls = 0

    override fun observe(userId: String): Flow<List<Obra>> = emptyFlow()
    override fun observeOne(userId: String, obraId: String): Flow<Obra?> = emptyFlow()
    override fun observePublicas(): Flow<List<Obra>> = emptyFlow()
    override fun observePublica(obraId: String): Flow<Obra?> = emptyFlow()
    override fun observeSuscripciones(userId: String): Flow<Set<String>> = emptyFlow()
    override suspend fun setSuscripcion(userId: String, obraId: String, seguir: Boolean): AppResult<Unit> =
        AppResult.Success(Unit)
    override suspend fun create(userId: String, draft: ObraDraft): AppResult<Obra> = error("Not needed")
    override suspend fun update(userId: String, obraId: String, draft: ObraDraft): AppResult<Obra> = error("Not needed")

    override suspend fun existsAndOwnedByCurrentUser(obraId: String): AppResult<Boolean> {
        existsCalls++
        return existsResult
    }

    override suspend fun delete(obraId: String): AppResult<Unit> {
        deleteCalls++
        return deleteResult
    }

    override suspend fun deleteAllSuscriptoresByObra(obraId: String): AppResult<Unit> {
        deleteAllSuscriptoresCalls++
        return deleteAllSuscriptoresResult
    }
}

open class FakeRelevamientoRepo(
    var deleteResult: AppResult<Unit> = AppResult.Success(Unit),
    var deleteAllResult: AppResult<Unit> = AppResult.Success(Unit)
) : RelevamientoRepository {
    var deleteCalls = 0
    var deleteAllCalls = 0

    override fun observeAll(userId: String): Flow<List<Relevamiento>> = emptyFlow()
    override fun observe(userId: String, obraId: String): Flow<List<Relevamiento>> = emptyFlow()
    override fun observeByObra(obraId: String): Flow<List<Relevamiento>> = emptyFlow()
    override fun observeByTarea(obraId: String, tareaId: String): Flow<List<Relevamiento>> = emptyFlow()
    override suspend fun create(userId: String, obraId: String, draft: RelevamientoDraft, photo: ByteArray?): AppResult<Relevamiento> = error("Not needed")
    override suspend fun update(userId: String, obraId: String, relevamientoId: String, draft: RelevamientoDraft, photo: ByteArray?): AppResult<Relevamiento> = error("Not needed")
    override suspend fun delete(obraId: String, relevamientoId: String): AppResult<Unit> {
        deleteCalls++
        return deleteResult
    }

    override suspend fun deleteAllByObra(obraId: String): AppResult<Unit> {
        deleteAllCalls++
        return deleteAllResult
    }
}

open class FakeTareaRepo(
    var deleteAllResult: AppResult<Unit> = AppResult.Success(Unit)
) : TareaRepository {
    var deleteAllCalls = 0

    override fun observeByObra(obraId: String): Flow<List<Tarea>> = emptyFlow()
    override fun observeOne(obraId: String, tareaId: String): Flow<Tarea?> = emptyFlow()
    override suspend fun create(userId: String, obraId: String, draft: TareaDraft): AppResult<Tarea> = error("Not needed")
    override suspend fun update(userId: String, obraId: String, tareaId: String, draft: TareaDraft): AppResult<Tarea> = error("Not needed")
    override suspend fun delete(obraId: String, tareaId: String, vinculados: RelevamientosVinculados): AppResult<Unit> = error("Not needed")

    override suspend fun deleteAllByObra(obraId: String): AppResult<Unit> {
        deleteAllCalls++
        return deleteAllResult
    }
}
