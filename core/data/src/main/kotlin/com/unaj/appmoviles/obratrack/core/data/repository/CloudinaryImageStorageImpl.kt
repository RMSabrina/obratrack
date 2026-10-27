package com.unaj.appmoviles.obratrack.core.data.repository

import com.cloudinary.Cloudinary
import com.cloudinary.utils.ObjectUtils
import com.unaj.appmoviles.obratrack.core.domain.error.AppResult
import com.unaj.appmoviles.obratrack.core.domain.error.RelevamientoError
import com.unaj.appmoviles.obratrack.core.domain.repository.ImageStorageRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.unaj.appmoviles.obratrack.core.data.di.CloudinaryUploadPreset
import android.util.Log
import java.io.IOException
import java.net.SocketTimeoutException
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "CloudinaryImageStorage"

/** Sin red o sin respuesta a tiempo se distinguen; cualquier otra falla es desconocida. */
internal fun Exception.toUploadError(): RelevamientoError = when (this) {
    is SocketTimeoutException -> RelevamientoError.Timeout
    is IOException -> RelevamientoError.Red
    else -> RelevamientoError.Desconocido
}

@Singleton
class CloudinaryImageStorageImpl @Inject constructor(
    private val cloudinary: Cloudinary,
    @CloudinaryUploadPreset private val uploadPreset: String
) : ImageStorageRepository {

    override suspend fun uploadImage(bytes: ByteArray, folder: String, name: String): AppResult<String> {
        return withContext(Dispatchers.IO) {
            try {
                // Achicar la foto es pesado: va acá, en IO, y no en el hilo principal.
                val foto = when (val preparada = prepararFoto(bytes)) {
                    is FotoPreparada.Lista -> preparada.bytes
                    FotoPreparada.DemasiadoGrande -> return@withContext AppResult.Error(RelevamientoError.ImagenMuyGrande)
                }
                // Configuramos la subida "sin firma" usando el upload_preset
                // y definiendo la carpeta ("folder") y el ID público ("public_id")
                val params = ObjectUtils.asMap(
                    "upload_preset", uploadPreset,
                    "folder", folder,
                    "public_id", name
                )
                
                // Realizamos la subida
                val result = cloudinary.uploader().unsignedUpload(foto, uploadPreset, params)
                
                // Retornamos la URL segura de la imagen
                val secureUrl = result["secure_url"] as? String
                if (secureUrl != null) {
                    AppResult.Success(secureUrl)
                } else {
                    AppResult.Error(RelevamientoError.Desconocido)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: OutOfMemoryError) {
                // Foto enorme que no entra en memoria al decodificarla: no es un fallo de red.
                Log.e(TAG, "Sin memoria para preparar la imagen", e)
                AppResult.Error(RelevamientoError.ImagenMuyGrande)
            } catch (e: Exception) {
                Log.e(TAG, "Fallo al subir la imagen", e)
                AppResult.Error(e.toUploadError())
            }
        }
    }
}
