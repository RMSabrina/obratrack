package com.unaj.appmoviles.obratrack

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.unaj.appmoviles.features.obras.export.limpiarPdfsTemporales
import com.unaj.appmoviles.obratrack.core.domain.repository.LanguagePreferencesRepository
import com.unaj.appmoviles.obratrack.core.domain.repository.ThemePreferencesRepository
import com.unaj.appmoviles.obratrack.notification.NotificationHelper
import com.unaj.appmoviles.obratrack.ui.theme.aNightMode
import com.unaj.appmoviles.obratrack.worker.AvisosVisitaSincronizador
import com.unaj.appmoviles.obratrack.worker.RelevamientoReminderWorker
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class ObraTrackApplication : Application(), Configuration.Provider {
    
    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var avisosVisitaSincronizador: AvisosVisitaSincronizador
    @Inject lateinit var temaPreferences: ThemePreferencesRepository
    @Inject lateinit var languagePreferences: LanguagePreferencesRepository

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        // Solo al arrancar: los recursos XML con calificador night (DayNight) coinciden con el tema elegido desde la primera
        // pantalla. Cambiar el tema en vivo lo hace Compose, sin recrear la Activity.
        AppCompatDelegate.setDefaultNightMode(temaPreferences.getModo().aNightMode())
        // Deja el espejo del idioma al día con AppCompat (lo usa el Worker de avisos).
        languagePreferences.sincronizarEspejo()
        NotificationHelper.createNotificationChannels(this)
        RelevamientoReminderWorker.schedulePeriodic(this)
        avisosVisitaSincronizador.iniciar(appScope)
        // PDF viejos de la caché: ningún formulario los usa al arrancar.
        appScope.launch(Dispatchers.IO) { limpiarPdfsTemporales(cacheDir) }
    }
}
