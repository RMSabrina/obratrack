package com.unaj.appmoviles.obratrack.core.domain.repository

import com.unaj.appmoviles.obratrack.core.domain.model.AnticipacionAviso
import kotlinx.coroutines.flow.Flow

interface AvisoPreferencesRepository {
    fun observeAnticipacion(): Flow<AnticipacionAviso>
    fun getAnticipacion(): AnticipacionAviso
    suspend fun setAnticipacion(anticipacion: AnticipacionAviso)
}
