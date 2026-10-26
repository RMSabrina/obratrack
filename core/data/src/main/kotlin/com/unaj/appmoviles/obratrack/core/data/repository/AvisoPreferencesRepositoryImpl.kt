package com.unaj.appmoviles.obratrack.core.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.unaj.appmoviles.obratrack.core.domain.model.AnticipacionAviso
import com.unaj.appmoviles.obratrack.core.domain.repository.AvisoPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AvisoPreferencesRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context
) : AvisoPreferencesRepository {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("obratrack_aviso_prefs", Context.MODE_PRIVATE)

    private companion object {
        const val KEY_ANTICIPACION = "anticipacion"
    }

    override fun observeAnticipacion(): Flow<AnticipacionAviso> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_ANTICIPACION) trySend(getAnticipacion())
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(getAnticipacion())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    /** Valor ausente o desconocido: la anticipación por defecto. */
    override fun getAnticipacion(): AnticipacionAviso {
        val guardada = prefs.getString(KEY_ANTICIPACION, null)
        return AnticipacionAviso.entries.firstOrNull { it.name == guardada } ?: AnticipacionAviso.POR_DEFECTO
    }

    override suspend fun setAnticipacion(anticipacion: AnticipacionAviso) {
        prefs.edit().putString(KEY_ANTICIPACION, anticipacion.name).apply()
    }
}
