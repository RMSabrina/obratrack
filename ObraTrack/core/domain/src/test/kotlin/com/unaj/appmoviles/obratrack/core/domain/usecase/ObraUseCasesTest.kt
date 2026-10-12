package com.unaj.appmoviles.obratrack.core.domain.usecase

import com.unaj.appmoviles.obratrack.core.domain.error.AppResult
import com.unaj.appmoviles.obratrack.core.domain.error.ObraError
import com.unaj.appmoviles.obratrack.core.domain.model.EstadoObra
import com.unaj.appmoviles.obratrack.core.domain.model.Obra
import com.unaj.appmoviles.obratrack.core.domain.model.ObraDraft
import com.unaj.appmoviles.obratrack.core.domain.repository.ObraRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ObraUseCasesTest {

    @Test
    fun create_rechazaNombreVacio_yNoLlamaAlRepositorio() = runBlocking {
        val repository = FakeObraRepository()
        val result = CreateObraUseCase(repository)("uid", draft(nombre = "   "))

        assertEquals(ObraError.NombreVacio, (result as AppResult.Error).error)
        assertEquals(0, repository.createCalls)
    }

    @Test
    fun create_rechazaClienteVacio() = runBlocking {
        val repository = FakeObraRepository()
        val result = CreateObraUseCase(repository)("uid", draft(cliente = ""))

        assertEquals(ObraError.ClienteVacio, (result as AppResult.Error).error)
        assertEquals(0, repository.createCalls)
    }

    @Test
    fun create_rechazaDireccionVacia() = runBlocking {
        val repository = FakeObraRepository()
        val result = CreateObraUseCase(repository)("uid", draft(direccion = " "))

        assertEquals(ObraError.DireccionVacia, (result as AppResult.Error).error)
        assertEquals(0, repository.createCalls)
    }

    @Test
    fun create_recortaEspacios_yDelegaEnElRepositorio() = runBlocking {
        val repository = FakeObraRepository()
        val result = CreateObraUseCase(repository)(
            "uid",
            draft(nombre = "  Edificio  ", cliente = " ACME ", direccion = " Rivadavia 1 ")
        )

        assertTrue(result is AppResult.Success)
        assertEquals(1, repository.createCalls)
        assertEquals("Edificio", repository.lastDraft?.nombre)
        assertEquals("ACME", repository.lastDraft?.cliente)
        assertEquals("Rivadavia 1", repository.lastDraft?.direccion)
    }

    @Test
    fun update_rechazaNombreVacio_yNoLlamaAlRepositorio() = runBlocking {
        val repository = FakeObraRepository()
        val result = UpdateObraUseCase(repository)("uid", "obra-1", draft(nombre = ""))

        assertEquals(ObraError.NombreVacio, (result as AppResult.Error).error)
        assertEquals(0, repository.updateCalls)
    }

    @Test
    fun update_delegaElId_cuandoElBorradorEsValido() = runBlocking {
        val repository = FakeObraRepository()
        val result = UpdateObraUseCase(repository)("uid", "obra-1", draft())

        assertTrue(result is AppResult.Success)
        assertEquals("obra-1", repository.lastObraId)
        assertEquals("uid", repository.lastUserId)
    }

    private fun draft(
        nombre: String = "Edificio",
        cliente: String = "ACME",
        direccion: String = "Rivadavia 1"
    ) = ObraDraft(
        nombre = nombre,
        cliente = cliente,
        direccion = direccion,
        publica = false,
        estado = EstadoObra.ACTIVA
    )
}

private class FakeObraRepository : ObraRepository {
    var createCalls: Int = 0
    var updateCalls: Int = 0
    var lastDraft: ObraDraft? = null
    var lastUserId: String? = null
    var lastObraId: String? = null

    override fun observe(userId: String): Flow<List<Obra>> = emptyFlow()

    override suspend fun create(userId: String, draft: ObraDraft): AppResult<Obra> {
        createCalls += 1
        lastUserId = userId
        lastDraft = draft
        return AppResult.Success(draft.toObra(id = "nueva"))
    }

    override suspend fun update(userId: String, obraId: String, draft: ObraDraft): AppResult<Obra> {
        updateCalls += 1
        lastUserId = userId
        lastObraId = obraId
        lastDraft = draft
        return AppResult.Success(draft.toObra(id = obraId))
    }

    private fun ObraDraft.toObra(id: String) = Obra(
        id = id,
        userId = lastUserId.orEmpty(),
        nombre = nombre,
        cliente = cliente,
        direccion = direccion,
        publica = publica,
        estado = estado,
        fechaInicio = 0L,
        ultimaVisita = null
    )
}
