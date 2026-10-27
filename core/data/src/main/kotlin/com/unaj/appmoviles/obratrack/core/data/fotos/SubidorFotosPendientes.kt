package com.unaj.appmoviles.obratrack.core.data.fotos

import android.util.Log
import com.unaj.appmoviles.obratrack.core.domain.error.AppResult
import com.unaj.appmoviles.obratrack.core.domain.error.DomainError
import com.unaj.appmoviles.obratrack.core.domain.error.RelevamientoError
import com.unaj.appmoviles.obratrack.core.domain.repository.FotoRelevamientoRepository
import com.unaj.appmoviles.obratrack.core.domain.repository.ImageStorageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

enum class ResultadoSubida {
    /** Subida y asignada al relevamiento. */
    LISTO,

    /** Sin red o falla pasajera: el worker debe reintentar. */
    REINTENTAR,

    /** No hay nada que hacer: el relevamiento ya no existe o no hay foto. */
    DESCARTADA,

    /** La foto no se pudo subir y se borró (demasiado grande o intentos agotados): hay que avisar al usuario. */
    PERDIDA
}

/** La lógica de un trabajo de subida, separada de WorkManager para poder probarla. */
class SubidorFotosPendientes @Inject constructor(
    private val store: FotosPendientesStore,
    private val imagenes: ImageStorageRepository,
    private val relevamientos: FotoRelevamientoRepository
) {
    /** [intento] es la cantidad de intentos fallidos anteriores (0 en el primero). */
    suspend fun subir(obraId: String, relevamientoId: String, intento: Int): ResultadoSubida {
        val archivo = store.archivo(obraId, relevamientoId) ?: return ResultadoSubida.DESCARTADA

        // Antes de subir: si el relevamiento se borró, no se gasta la subida.
        when (val existe = relevamientos.existe(obraId, relevamientoId)) {
            is AppResult.Success -> if (!existe.data) return descartar(obraId, relevamientoId)
            is AppResult.Error -> return reintentarOSoltar(obraId, relevamientoId, existe.error, intento)
        }

        val bytes = withContext(Dispatchers.IO) { archivo.readBytes() }
        val url = when (
            val subida = imagenes.uploadImage(
                bytes = bytes,
                folder = "relevamientos/$obraId",
                // Nombre único: una subida sin firma no reemplaza una imagen existente.
                name = UUID.randomUUID().toString()
            )
        ) {
            is AppResult.Success -> subida.data
            is AppResult.Error -> return reintentarOSoltar(obraId, relevamientoId, subida.error, intento)
        }

        return when (val asignada = relevamientos.asignarFotoUrl(obraId, relevamientoId, url)) {
            is AppResult.Success -> {
                store.borrar(obraId, relevamientoId)
                if (asignada.data) ResultadoSubida.LISTO else ResultadoSubida.DESCARTADA
            }
            is AppResult.Error -> reintentarOSoltar(obraId, relevamientoId, asignada.error, intento)
        }
    }

    private fun descartar(obraId: String, relevamientoId: String): ResultadoSubida {
        store.borrar(obraId, relevamientoId)
        return ResultadoSubida.DESCARTADA
    }

    private fun perder(obraId: String, relevamientoId: String): ResultadoSubida {
        store.borrar(obraId, relevamientoId)
        return ResultadoSubida.PERDIDA
    }

    private fun reintentarOSoltar(
        obraId: String,
        relevamientoId: String,
        error: DomainError,
        intento: Int
    ): ResultadoSubida = when {
        // La foto no se puede achicar lo suficiente: reintentar no sirve.
        error == RelevamientoError.ImagenMuyGrande -> {
            Log.w(TAG, "Foto de $relevamientoId descartada: demasiado grande")
            perder(obraId, relevamientoId)
        }
        // Sin red no cuenta como intento fallido: se espera a que vuelva.
        error == RelevamientoError.Red || error == RelevamientoError.Timeout -> ResultadoSubida.REINTENTAR
        intento + 1 >= MAX_INTENTOS -> {
            Log.w(TAG, "Foto de $relevamientoId descartada tras ${intento + 1} intentos: $error")
            perder(obraId, relevamientoId)
        }
        else -> ResultadoSubida.REINTENTAR
    }

    companion object {
        private const val TAG = "SubidorFotosPendientes"
        const val MAX_INTENTOS = 5
    }
}
