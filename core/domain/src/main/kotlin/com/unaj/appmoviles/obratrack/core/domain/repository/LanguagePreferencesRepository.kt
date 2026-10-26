package com.unaj.appmoviles.obratrack.core.domain.repository

import com.unaj.appmoviles.obratrack.core.domain.model.AppLanguage
import kotlinx.coroutines.flow.Flow

interface LanguagePreferencesRepository {
    fun observeLanguage(): Flow<AppLanguage>
    fun getLanguage(): AppLanguage
    suspend fun setLanguage(language: AppLanguage)
}
