package com.unaj.appmoviles.obratrack.core.data.repository

import android.app.LocaleManager
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.ConfigurationCompat
import androidx.core.os.LocaleListCompat
import com.unaj.appmoviles.obratrack.core.domain.model.AppLanguage
import com.unaj.appmoviles.obratrack.core.domain.repository.LanguagePreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
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

    /** Avisa de los cambios hechos con [setLanguage] a quien esté observando la elección. */
    private val cambios = MutableSharedFlow<AppLanguage?>(extraBufferCapacity = 1)

    /**
     * Idioma que eligió el usuario para la app; null = "idioma del sistema" (sin elección). Sale de
     * AppCompat; solo en Android 12 o inferior, donde puede no tenerlo cargado antes de abrir una
     * Activity, se mira también el espejo.
     */
    override fun observeLanguageChoice(): Flow<AppLanguage?> = flow {
        emit(leerEleccion())
        emitAll(cambios)
    }

    private fun leerEleccion(): AppLanguage? = eleccionDeIdioma(
        idiomasApp = codigosDe(AppCompatDelegate.getApplicationLocales()),
        espejo = prefs.getString(KEY_LANGUAGE, null),
        usarEspejo = Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2
    )

    /**
     * Idioma para un Worker: no consulta a AppCompat (sin Activity no es confiable), usa el espejo
     * que [sincronizarEspejo] y [setLanguage] mantienen, y si no hay elección, el sistema.
     */
    override fun getBackgroundLanguage(): AppLanguage = idiomaEfectivo(
        idiomasApp = emptyList(),
        espejo = prefs.getString(KEY_LANGUAGE, null),
        idiomasSistema = codigosDeSistema(),
        usarEspejo = true
    )

    override fun sincronizarEspejo() {
        val actual = prefs.getString(KEY_LANGUAGE, null)
        val nuevo = espejoTrasSincronizar(
            idiomasApp = idiomasDeLaAppSinActivity(),
            espejoActual = actual,
            appCompatEsLaFuente = Build.VERSION.SDK_INT > Build.VERSION_CODES.S_V2
        )
        if (nuevo == actual) return
        prefs.edit().apply { if (nuevo == null) remove(KEY_LANGUAGE) else putString(KEY_LANGUAGE, nuevo) }.apply()
    }

    /** [language] null = "idioma del sistema": se borra el idioma de la app y el espejo. */
    override suspend fun setLanguage(language: AppLanguage?) {
        AppCompatDelegate.setApplicationLocales(
            if (language == null) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(language.code)
        )
        prefs.edit().apply {
            if (language == null) remove(KEY_LANGUAGE) else putString(KEY_LANGUAGE, language.code)
        }.apply()
        cambios.tryEmit(language)
    }

    private fun codigosDe(locales: LocaleListCompat): List<String> =
        (0 until locales.size()).mapNotNull { locales.get(it)?.language }

    /**
     * Idioma elegido para la app, leído sin depender de una Activity. En Android 13+ se le pregunta
     * al sistema: AppCompat necesita un contexto que todavía no existe en Application.onCreate y
     * devolvería vacío aunque haya idioma elegido.
     */
    private fun idiomasDeLaAppSinActivity(): List<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val locales = context.getSystemService(LocaleManager::class.java).applicationLocales
            (0 until locales.size()).mapNotNull { locales.get(it)?.language }
        } else {
            codigosDe(AppCompatDelegate.getApplicationLocales())
        }

    /** Idiomas del sistema, en orden de preferencia (no los de la app). */
    private fun codigosDeSistema(): List<String> =
        codigosDe(ConfigurationCompat.getLocales(Resources.getSystem().configuration))
}

/**
 * Idioma efectivo de la app. Orden: el de la app (AppCompat); el espejo, si [usarEspejo]; el primer
 * idioma del sistema que la app soporta (igual que Android al elegir los recursos); y español, el
 * idioma por defecto, si el sistema usa uno que la app no tiene. Un idioma de la app que no sea
 * es/en/it también cae en español, porque los recursos caen en los valores por defecto.
 */
internal fun idiomaEfectivo(
    idiomasApp: List<String>,
    espejo: String?,
    idiomasSistema: List<String>,
    usarEspejo: Boolean
): AppLanguage {
    idiomasApp.firstOrNull()?.let { return idiomaDeCodigo(it) ?: AppLanguage.SPANISH }
    if (usarEspejo) idiomaDeCodigo(espejo)?.let { return it }
    idiomasSistema.firstNotNullOfOrNull { idiomaDeCodigo(it) }?.let { return it }
    return AppLanguage.SPANISH
}

/**
 * Valor que debe quedar en el espejo al arrancar (null = borrarlo). Con idioma elegido en la app,
 * se guarda. Sin elección: en Android 13+ AppCompat es la fuente de verdad, así que el espejo se
 * borra (si no, un idioma viejo reaparecería al volver a "idioma del sistema"); en Android 12 o
 * inferior vacío puede ser "todavía no cargado", y el espejo se conserva.
 */
internal fun espejoTrasSincronizar(
    idiomasApp: List<String>,
    espejoActual: String?,
    appCompatEsLaFuente: Boolean
): String? {
    val elegido = idiomasApp.firstOrNull()
    if (elegido != null) return idiomaDeCodigo(elegido)?.code
    return if (appCompatEsLaFuente) null else espejoActual
}

/** Idioma de la app para un código ("es", "en" o "it", con o sin región); null si no es uno de ellos. */
internal fun idiomaDeCodigo(codigo: String?): AppLanguage? =
    AppLanguage.entries.firstOrNull { codigo != null && codigo.substringBefore('-').equals(it.code, ignoreCase = true) }

/**
 * Lo que eligió el usuario en la app (null = "idioma del sistema"): el idioma de la app si es uno de
 * los nuestros; si no, el espejo cuando [usarEspejo]. A diferencia de [idiomaEfectivo], no mira al sistema.
 */
internal fun eleccionDeIdioma(idiomasApp: List<String>, espejo: String?, usarEspejo: Boolean): AppLanguage? {
    idiomasApp.firstOrNull()?.let { return idiomaDeCodigo(it) }
    return if (usarEspejo) idiomaDeCodigo(espejo) else null
}
