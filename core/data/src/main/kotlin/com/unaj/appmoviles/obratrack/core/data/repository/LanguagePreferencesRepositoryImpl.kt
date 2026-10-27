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

    /**
     * La fuente de verdad es el idioma de la app que guarda AppCompat (y el sistema en Android 13+).
     * Las preferencias son solo un espejo para cuando AppCompat no lo conoce: un Worker en
     * Android 12 o inferior, donde no hay Activity que lo cargue.
     */
    override fun getLanguage(): AppLanguage {
        val deLaApp = AppCompatDelegate.getApplicationLocales()
        if (!deLaApp.isEmpty) return idiomaDeCodigo(deLaApp.get(0)?.language) ?: AppLanguage.SPANISH
        return idiomaDeCodigo(prefs.getString(KEY_LANGUAGE, null)) ?: AppLanguage.SPANISH
    }

    override suspend fun setLanguage(language: AppLanguage) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language.code))
        prefs.edit().putString(KEY_LANGUAGE, language.code).apply()
    }
}

/** Idioma de la app para un código ("es", "en" o "it", con o sin región); null si no es uno de ellos. */
internal fun idiomaDeCodigo(codigo: String?): AppLanguage? =
    AppLanguage.entries.firstOrNull { codigo != null && codigo.substringBefore('-').equals(it.code, ignoreCase = true) }
