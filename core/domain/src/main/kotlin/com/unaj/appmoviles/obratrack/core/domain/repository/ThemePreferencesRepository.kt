package com.unaj.appmoviles.obratrack.core.domain.repository

import com.unaj.appmoviles.obratrack.core.domain.model.ModoTema
import kotlinx.coroutines.flow.Flow

interface ThemePreferencesRepository {
    fun observeModo(): Flow<ModoTema>

    /** Lectura síncrona: se usa antes del primer cuadro para que la app no abra con el tema equivocado. */
    fun getModo(): ModoTema

    suspend fun setModo(modo: ModoTema)
}
