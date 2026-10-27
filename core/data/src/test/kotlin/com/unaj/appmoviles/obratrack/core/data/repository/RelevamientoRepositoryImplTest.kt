package com.unaj.appmoviles.obratrack.core.data.repository

import com.unaj.appmoviles.obratrack.core.data.fotos.PreparadorFotos
import com.unaj.appmoviles.obratrack.core.domain.error.AppResult
import com.unaj.appmoviles.obratrack.core.domain.error.RelevamientoError
import com.unaj.appmoviles.obratrack.core.domain.model.RelevamientoDraft
import com.unaj.appmoviles.obratrack.core.domain.repository.ColaFotosPendientes
import com.unaj.appmoviles.obratrack.core.firebase.FirebaseResult
import com.unaj.appmoviles.obratrack.core.firebase.FirestoreRelevamientoDataSource
import com.unaj.appmoviles.obratrack.core.firebase.RelevamientoDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RelevamientoRepositoryImplTest {

    private val draft = RelevamientoDraft(fecha = 1_700_000_000_000L, nota = "Avance")
    private val dataSource = FakeRelevamientoDataSource()
    private val cola = FakeCola()
    private var preparar: (ByteArray) -> ByteArray? = { it }
    private val preparador = object : PreparadorFotos {
        override suspend fun preparar(bytes: ByteArray): ByteArray? = preparar.invoke(bytes)
    }
    private val repository = RelevamientoRepositoryImpl(dataSource, cola, preparador)

    @Test
    fun crearConFoto_guardaElRelevamientoYEncolaLaFotoSinSubirla() = runBlocking {
        val res = repository.create("uid", "obra-1", draft, photo = byteArrayOf(1, 2))

        assertTrue(res is AppResult.Success)
        val creado = dataSource.creados.single()
        // El documento se guarda sin foto: la URL se completa cuando la subida termina.
        assertNull(creado.fotoUrl)
        assertEquals(listOf(Triple("obra-1", "nuevo-id", 2)), cola.encoladas)
    }

    @Test
    fun crearSinFoto_noEncolaNada() = runBlocking {
        repository.create("uid", "obra-1", draft, photo = null)

        assertEquals(emptyList<Triple<String, String, Int>>(), cola.encoladas)
    }

    @Test
    fun fotoDemasiadoGrande_noGuardaNada() = runBlocking {
        preparar = { null }

        val res = repository.create("uid", "obra-1", draft, photo = byteArrayOf(1))

        assertEquals(AppResult.Error(RelevamientoError.ImagenMuyGrande), res)
        assertTrue(dataSource.creados.isEmpty())
    }

    @Test
    fun siNoSePuedeGuardarLaFoto_seDeshaceElAltaParaQueReintente() = runBlocking {
        cola.falla = true

        val res = repository.create("uid", "obra-1", draft, photo = byteArrayOf(1))

        assertEquals(AppResult.Error(RelevamientoError.Desconocido), res)
        assertEquals(listOf("nuevo-id"), dataSource.borrados)
    }

    @Test
    fun editarConOtraFoto_reemplazaLaQueEstabaEnCola() = runBlocking {
        repository.update("uid", "obra-1", "r1", draft, photo = byteArrayOf(1))
        repository.update("uid", "obra-1", "r1", draft, photo = byteArrayOf(2, 2))

        assertEquals(listOf(Triple("obra-1", "r1", 1), Triple("obra-1", "r1", 2)), cola.encoladas)
        assertTrue(cola.canceladas.isEmpty())
    }

    @Test
    fun editarSinTocarLaFotoPendiente_noLaCancelaNiToqueElCampoDeFoto() = runBlocking {
        val conFotoLocal = draft.copy(fotoUrl = "file:///data/fotos_pendientes/obra-1/r1.jpg")

        repository.update("uid", "obra-1", "r1", conFotoLocal, photo = null)

        val enviado = dataSource.actualizados.single()
        assertTrue(enviado.conservarFoto)
        // La ruta local no se guarda nunca en Firestore.
        assertNull(enviado.fotoUrl)
        assertTrue(cola.canceladas.isEmpty())
        assertTrue(cola.encoladas.isEmpty())
    }

    @Test
    fun quitarLaFoto_cancelaLaQueEstabaEsperando() = runBlocking {
        repository.update("uid", "obra-1", "r1", draft.copy(fotoUrl = null), photo = null)

        assertFalse(dataSource.actualizados.single().conservarFoto)
        assertEquals(listOf("obra-1" to "r1"), cola.canceladas)
    }

    @Test
    fun editarConLaFotoRemotaIntacta_laConservaSinCancelarNada() = runBlocking {
        repository.update("uid", "obra-1", "r1", draft.copy(fotoUrl = "https://img/a"), photo = null)

        assertEquals("https://img/a", dataSource.actualizados.single().fotoUrl)
        assertTrue(cola.canceladas.isEmpty())
    }

    @Test
    fun borrarElRelevamiento_cancelaSuFotoEnCola() = runBlocking {
        repository.delete("obra-1", "r1")

        assertEquals(listOf("obra-1" to "r1"), cola.canceladas)
    }

    @Test
    fun siElBorradoFalla_noSeCancelaLaFoto() = runBlocking {
        dataSource.fallaBorrado = true

        repository.delete("obra-1", "r1")

        assertTrue(cola.canceladas.isEmpty())
    }

    @Test
    fun borrarLaObra_cancelaLasFotosDeTodaLaObra() = runBlocking {
        repository.deleteAllByObra("obra-1")

        assertEquals(listOf("obra-1"), cola.obrasCanceladas)
    }

    @Test
    fun unRelevamientoConFotoEnCola_seVeDesdeElArchivoLocalYMarcadoPendiente() = runBlocking {
        dataSource.lista.value = listOf(dto("r1"), dto("r2", fotoUrl = "https://img/b"))
        cola.estado.value = mapOf("r1" to "/data/fotos_pendientes/obra-1/r1.jpg")

        val lista = repository.observeByObra("obra-1").first().associateBy { it.id }

        assertEquals("file:///data/fotos_pendientes/obra-1/r1.jpg", lista.getValue("r1").fotoUrl)
        assertTrue(lista.getValue("r1").fotoPendiente)
        assertEquals("https://img/b", lista.getValue("r2").fotoUrl)
        assertFalse(lista.getValue("r2").fotoPendiente)
    }

    private fun dto(id: String, fotoUrl: String? = null) = RelevamientoDto(
        id = id, obraId = "obra-1", userId = "uid", fechaMillis = 1L, nota = "n", fotoUrl = fotoUrl
    )

    private class FakeCola : ColaFotosPendientes {
        val estado = MutableStateFlow<Map<String, String>>(emptyMap())
        val encoladas = mutableListOf<Triple<String, String, Int>>()
        val canceladas = mutableListOf<Pair<String, String>>()
        val obrasCanceladas = mutableListOf<String>()
        var falla = false

        override val pendientes: Flow<Map<String, String>> = estado

        override suspend fun encolar(obraId: String, relevamientoId: String, bytes: ByteArray) {
            if (falla) throw java.io.IOException("disco lleno")
            encoladas += Triple(obraId, relevamientoId, bytes.size)
        }

        override suspend fun cancelar(obraId: String, relevamientoId: String) {
            canceladas += obraId to relevamientoId
        }

        override suspend fun cancelarDeObra(obraId: String) {
            obrasCanceladas += obraId
        }
    }

    private class FakeRelevamientoDataSource : FirestoreRelevamientoDataSource {
        val creados = mutableListOf<RelevamientoDto>()
        val actualizados = mutableListOf<RelevamientoDto>()
        val borrados = mutableListOf<String>()
        val lista = MutableStateFlow<List<RelevamientoDto>>(emptyList())
        var fallaBorrado = false

        override fun observeAll(userId: String): Flow<List<RelevamientoDto>> = emptyFlow()
        override fun observe(userId: String, obraId: String): Flow<List<RelevamientoDto>> = emptyFlow()
        override fun observeByObra(obraId: String): Flow<List<RelevamientoDto>> = lista
        override fun observeByTarea(obraId: String, tareaId: String): Flow<List<RelevamientoDto>> = emptyFlow()

        override suspend fun create(dto: RelevamientoDto, photo: ByteArray?): FirebaseResult<RelevamientoDto> {
            val guardado = dto.copy(id = "nuevo-id")
            creados += guardado
            return FirebaseResult.Success(guardado)
        }

        override suspend fun update(userId: String, dto: RelevamientoDto, photo: ByteArray?): FirebaseResult<RelevamientoDto> {
            actualizados += dto
            return FirebaseResult.Success(dto)
        }

        override suspend fun delete(obraId: String, relevamientoId: String): FirebaseResult<Unit> {
            if (fallaBorrado) return FirebaseResult.Error(IllegalStateException("falló"))
            borrados += relevamientoId
            return FirebaseResult.Success(Unit)
        }

        override suspend fun deleteAllByObra(obraId: String): FirebaseResult<Unit> = FirebaseResult.Success(Unit)
    }
}
