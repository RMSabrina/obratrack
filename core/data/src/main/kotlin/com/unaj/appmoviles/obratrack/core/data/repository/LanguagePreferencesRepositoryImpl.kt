package com.unaj.appmoviles.obratrack.core.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.unaj.appmoviles.obratrack.core.domain.model.AppLanguage
import com.unaj.appmoviles.obratrack.core.domain.repository.LanguagePreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LanguagePreferencesRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : LanguagePreferencesRepository {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("obratrack_language_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_LANGUAGE = "selected_language"
    }

    override fun observeLanguage(): Flow<AppLanguage> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_LANGUAGE) {
                trySend(getLanguage())
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(getLanguage())
        awaitClose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    override fun getLanguage(): AppLanguage {
        val saved = prefs.getString(KEY_LANGUAGE, null)
        if (saved != null) {
            return AppLanguage.entries.firstOrNull { it.code == saved } ?: AppLanguage.SPANISH
        }
        val currentLocales = AppCompatDelegate.getApplicationLocales()
        if (!currentLocales.isEmpty) {
            val tag = currentLocales.get(0)?.language.orEmpty()
            if (tag.startsWith("en", ignoreCase = true)) {
                return AppLanguage.ENGLISH
            }
            if (tag.startsWith("it", ignoreCase = true)) {
                return AppLanguage.ITALIAN
            }
        }
        return AppLanguage.SPANISH
    }

    override suspend fun setLanguage(language: AppLanguage) {
        prefs.edit().putString(KEY_LANGUAGE, language.code).apply()
        val appLocale = LocaleListCompat.forLanguageTags(language.code)
        AppCompatDelegate.setApplicationLocales(appLocale)
    }
}
