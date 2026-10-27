package com.unaj.appmoviles.obratrack.core.data.repository

import android.util.Log
import com.unaj.appmoviles.obratrack.core.data.fotos.PreparadorFotos
import com.unaj.appmoviles.obratrack.core.domain.error.AppResult
import com.unaj.appmoviles.obratrack.core.domain.error.RelevamientoError
import com.unaj.appmoviles.obratrack.core.domain.model.Relevamiento
import com.unaj.appmoviles.obratrack.core.domain.model.RelevamientoDraft
import com.unaj.appmoviles.obratrack.core.domain.repository.ColaFotosPendientes
import com.unaj.appmoviles.obratrack.core.domain.repository.RelevamientoRepository
import com.unaj.appmoviles.obratrack.core.firebase.FirebaseResult
import com.unaj.appmoviles.obratrack.core.firebase.FirestoreRelevamientoDataSource
import com.unaj.appmoviles.obratrack.core.firebase.RelevamientoDto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "RelevamientoRepository"
private const val PREFIJO_FOTO_LOCAL = "file://"

/** Una foto que todavía no se subió se ve desde su archivo local; nunca se guarda esa ruta en Firestore. */
internal fun String?.esFotoLocal(): Boolean = this?.startsWith(PREFIJO_FOTO_LOCAL) == true

internal fun Relevamiento.conFotoPendiente(rutaLocal: String?): Relevamiento =
    if (rutaLocal == null) this else copy(fotoUrl = PREFIJO_FOTO_LOCAL + rutaLocal, fotoPendiente = true)

@Singleton
class RelevamientoRepositoryImpl @Inject constructor(
    private val remoteDataSource: FirestoreRelevamientoDataSource,
    private val cola: ColaFotosPendientes,
    private val preparadorFotos: PreparadorFotos
) : RelevamientoRepository {

    override fun observeAll(userId: String): Flow<List<Relevamiento>> =
        remoteDataSource.observeAll(userId).conFotosPendientes()
            .conErrorDeLectura(Exception::toRelevamientoError)

    override fun observe(userId: String, obraId: String): Flow<List<Relevamiento>> =
        remoteDataSource.observe(userId, obraId).conFotosPendientes()
            .conErrorDeLectura(Exception::toRelevamientoError)

    override fun observeByObra(obraId: String): Flow<List<Relevamiento>> =
        remoteDataSource.observeByObra(obraId).conFotosPendientes()
            .conErrorDeLectura(Exception::toRelevamientoError)

    override fun observeByTarea(obraId: String, tareaId: String): Flow<List<Relevamiento>> =
        remoteDataSource.observeByTarea(obraId, tareaId).conFotosPendientes()
            .conErrorDeLectura(Exception::toRelevamientoError)

    private fun Flow<List<RelevamientoDto>>.conFotosPendientes(): Flow<List<Relevamiento>> =
        combine(this, cola.pendientes.catch { emit(emptyMap()) }) { relevamientos, pendientes ->
            relevamientos
                .map { dto -> dto.toDomain().conFotoPendiente(pendientes[dto.id]) }
                .sortedByDescending { it.fecha }
        }

    override suspend fun create(
        userId: String,
        obraId: String,
        draft: RelevamientoDraft,
        photo: ByteArray?
    ): AppResult<Relevamiento> {
        val foto = if (photo != null) {
            preparadorFotos.preparar(photo) ?: return AppResult.Error(RelevamientoError.ImagenMuyGrande)
        } else {
            null
        }
        // El relevamiento se guarda ya (en el teléfono) y la foto se sube cuando haya red.
        val creado = when (val res = remoteDataSource.create(draft.toDto(userId, obraId), null)) {
            is FirebaseResult.Success -> res.data
            is FirebaseResult.Error -> return AppResult.Error(res.exception.toRelevamientoError())
        }
        if (foto != null && !encolar(obraId, creado.id, foto)) {
            // Sin la foto en cola el relevamiento quedaría incompleto sin aviso: se deshace para que reintente.
            remoteDataSource.delete(obraId, creado.id)
            return AppResult.Error(RelevamientoError.Desconocido)
        }
        return AppResult.Success(creado.toDomain())
    }

    override suspend fun update(
        userId: String,
        obraId: String,
        relevamientoId: String,
        draft: RelevamientoDraft,
        photo: ByteArray?
    ): AppResult<Relevamiento> {
        val foto = if (photo != null) {
            preparadorFotos.preparar(photo) ?: return AppResult.Error(RelevamientoError.ImagenMuyGrande)
        } else {
            null
        }
        // Sin foto nueva y con la foto local del formulario, no se toca el campo de la foto.
        val conservarFoto = photo == null && draft.fotoUrl.esFotoLocal()
        val dto = draft.copy(fotoUrl = if (conservarFoto) null else draft.fotoUrl)
            .toDto(userId, obraId)
            .copy(id = relevamientoId, conservarFoto = conservarFoto)
        val actualizado = when (val res = remoteDataSource.update(userId, dto, null)) {
            is FirebaseResult.Success -> res.data
            is FirebaseResult.Error -> return AppResult.Error(res.exception.toRelevamientoError())
        }
        when {
            foto != null -> if (!encolar(obraId, relevamientoId, foto)) {
                return AppResult.Error(RelevamientoError.Desconocido)
            }
            // Se le quitó la foto: si había una esperando subirse, ya no hace falta.
            !conservarFoto && draft.fotoUrl == null -> cola.cancelar(obraId, relevamientoId)
        }
        return AppResult.Success(actualizado.toDomain())
    }

    private suspend fun encolar(obraId: String, relevamientoId: String, foto: ByteArray): Boolean = try {
        cola.encolar(obraId, relevamientoId, foto)
        true
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.e(TAG, "No se pudo guardar la foto para subirla", e)
        false
    }

    override suspend fun delete(obraId: String, relevamientoId: String): AppResult<Unit> {
        return when (val res = remoteDataSource.delete(obraId, relevamientoId)) {
            is FirebaseResult.Success -> {
                cola.cancelar(obraId, relevamientoId)
                AppResult.Success(Unit)
            }
            is FirebaseResult.Error -> AppResult.Error(res.exception.toRelevamientoError())
        }
    }

    override suspend fun deleteAllByObra(obraId: String): AppResult<Unit> {
        return when (val res = remoteDataSource.deleteAllByObra(obraId)) {
            is FirebaseResult.Success -> {
                cola.cancelarDeObra(obraId)
                AppResult.Success(Unit)
            }
            is FirebaseResult.Error -> AppResult.Error(res.exception.toRelevamientoError())
        }
    }
}
