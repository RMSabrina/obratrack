package com.unaj.appmoviles.obratrack.core.data.repository

import com.cloudinary.Cloudinary
import com.cloudinary.utils.ObjectUtils
import com.unaj.appmoviles.obratrack.core.domain.error.AppResult
import com.unaj.appmoviles.obratrack.core.domain.error.DomainError
import com.unaj.appmoviles.obratrack.core.domain.repository.ImageStorageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Error genérico para subida de imágenes.
 */
data object ImageUploadError : DomainError

class CloudinaryImageStorageImpl(
    private val cloudinary: Cloudinary,
    private val uploadPreset: String
) : ImageStorageRepository {

    override suspend fun uploadImage(bytes: ByteArray, folder: String, name: String): AppResult<String> {
        return withContext(Dispatchers.IO) {
            try {
                // Configuramos la subida "sin firma" usando el upload_preset
                // y definiendo la carpeta ("folder") y el ID público ("public_id")
                val params = ObjectUtils.asMap(
                    "upload_preset", uploadPreset,
                    "folder", folder,
                    "public_id", name
                )
                
                // Realizamos la subida
                val result = cloudinary.uploader().unsignedUpload(bytes, uploadPreset, params)
                
                // Retornamos la URL segura de la imagen
                val secureUrl = result["secure_url"] as? String
                if (secureUrl != null) {
                    AppResult.Success(secureUrl)
                } else {
                    AppResult.Error(ImageUploadError)
                }
            } catch (e: Exception) {
                AppResult.Error(ImageUploadError)
            }
        }
    }
}
