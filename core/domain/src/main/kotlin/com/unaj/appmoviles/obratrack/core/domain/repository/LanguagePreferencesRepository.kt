package com.unaj.appmoviles.obratrack.core.domain.repository

import com.unaj.appmoviles.obratrack.core.domain.model.AppLanguage
import kotlinx.coroutines.flow.Flow

interface LanguagePreferencesRepository {
    fun observeLanguage(): Flow<AppLanguage>
    fun getLanguage(): AppLanguage

    /** Idioma para trabajos en segundo plano (Worker): no depende de AppCompat, usa el espejo sincronizado. */
    fun getBackgroundLanguage(): AppLanguage

    /** Deja el espejo del idioma al día con AppCompat; se llama al arrancar la app. */
    fun sincronizarEspejo()
    suspend fun setLanguage(language: AppLanguage)
}
