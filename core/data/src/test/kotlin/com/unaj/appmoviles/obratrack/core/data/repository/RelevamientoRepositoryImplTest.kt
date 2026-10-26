package com.unaj.appmoviles.obratrack.core.data.repository

import com.unaj.appmoviles.obratrack.core.domain.error.AppResult
import com.unaj.appmoviles.obratrack.core.domain.model.RelevamientoDraft
import com.unaj.appmoviles.obratrack.core.domain.repository.ImageStorageRepository
import com.unaj.appmoviles.obratrack.core.firebase.FirebaseResult
import com.unaj.appmoviles.obratrack.core.firebase.FirestoreRelevamientoDataSource
import com.unaj.appmoviles.obratrack.core.firebase.RelevamientoDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class RelevamientoRepositoryImplTest {

    private val draft = RelevamientoDraft(fecha = 1_700_000_000_000L, nota = "Avance")

    @Test
    fun reemplazarLaFotoDosVeces_subeConNombresDistintos() = runBlocking {
        val storage = FakeImageStorage()
        val dataSource = FakeRelevamientoDataSource()
        val repository = RelevamientoRepositoryImpl(dataSource, storage)

        repository.update("uid", "obra-1", "r1", draft, photo = byteArrayOf(1))
        repository.update("uid", "obra-1", "r1", draft, photo = byteArrayOf(2))

        val (primera, segunda) = storage.subidas
        // Con el mismo nombre, Cloudinary devolvería la imagen vieja en vez de guardar la nueva.
        assertNotEquals(primera.name, segunda.name)
        assertEquals("relevamientos/obra-1", primera.folder)
        // La URL que se guarda es la de la subida nueva.
        assertEquals("https://img/${segunda.name}", dataSource.actualizados.last().fotoUrl)
    }

    private data class Subida(val folder: String, val name: String)

    private class FakeImageStorage : ImageStorageRepository {
        val subidas = mutableListOf<Subida>()

        override suspend fun uploadImage(bytes: ByteArray, folder: String, name: String): AppResult<String> {
            subidas += Subida(folder, name)
            return AppResult.Success("https://img/$name")
        }
    }

    private class FakeRelevamientoDataSource : FirestoreRelevamientoDataSource {
        val actualizados = mutableListOf<RelevamientoDto>()

        override fun observeAll(userId: String): Flow<List<RelevamientoDto>> = emptyFlow()
        override fun observe(userId: String, obraId: String): Flow<List<RelevamientoDto>> = emptyFlow()
        override fun observeByObra(obraId: String): Flow<List<RelevamientoDto>> = emptyFlow()
        override fun observeByTarea(obraId: String, tareaId: String): Flow<List<RelevamientoDto>> = emptyFlow()
        override suspend fun create(dto: RelevamientoDto, photo: ByteArray?): FirebaseResult<RelevamientoDto> =
            FirebaseResult.Success(dto)

        override suspend fun update(userId: String, dto: RelevamientoDto, photo: ByteArray?): FirebaseResult<RelevamientoDto> {
            actualizados += dto
            return FirebaseResult.Success(dto)
        }
        override suspend fun delete(obraId: String, relevamientoId: String): FirebaseResult<Unit> =
            FirebaseResult.Success(Unit)

        override suspend fun deleteAllByObra(obraId: String): FirebaseResult<Unit> = FirebaseResult.Success(Unit)
    }
}
