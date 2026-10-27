package com.unaj.appmoviles.obratrack.core.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.unaj.appmoviles.obratrack.core.domain.model.ModoTema
import com.unaj.appmoviles.obratrack.core.domain.repository.ThemePreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThemePreferencesRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context
) : ThemePreferencesRepository {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("obratrack_theme_prefs", Context.MODE_PRIVATE)

    private companion object {
        const val KEY_MODO = "modo"
    }

    override fun observeModo(): Flow<ModoTema> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_MODO) trySend(getModo())
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(getModo())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    /** Valor ausente o desconocido: sigue al sistema. */
    override fun getModo(): ModoTema = ModoTema.desdeNombre(prefs.getString(KEY_MODO, null))

    override suspend fun setModo(modo: ModoTema) {
        prefs.edit().putString(KEY_MODO, modo.name).apply()
    }
}
