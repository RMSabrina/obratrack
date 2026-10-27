package com.unaj.appmoviles.obratrack.core.domain.repository

import com.unaj.appmoviles.obratrack.core.domain.model.AppLanguage
import kotlinx.coroutines.flow.Flow

interface LanguagePreferencesRepository {
    /** Idioma que eligió el usuario para la app; null = "idioma del sistema" (sin elección). */
    fun observeLanguageChoice(): Flow<AppLanguage?>

    /** Idioma para trabajos en segundo plano (Worker): no depende de AppCompat, usa el espejo sincronizado. */
    fun getBackgroundLanguage(): AppLanguage

    /** Deja el espejo del idioma al día con AppCompat; se llama al arrancar la app. */
    fun sincronizarEspejo()

    /** [language] null = volver a seguir el idioma del sistema. */
    suspend fun setLanguage(language: AppLanguage?)
}
