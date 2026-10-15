package com.unaj.appmoviles.obratrack.core.domain.repository

import com.unaj.appmoviles.obratrack.core.domain.error.AppResult

interface ImageStorageRepository {
    suspend fun uploadImage(bytes: ByteArray, folder: String, name: String): AppResult<String>
}
