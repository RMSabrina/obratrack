package com.unaj.appmoviles.obratrack

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.unaj.appmoviles.features.obras.export.limpiarPdfsTemporales
import com.unaj.appmoviles.obratrack.notification.NotificationHelper
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

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannels(this)
        RelevamientoReminderWorker.schedulePeriodic(this)
        avisosVisitaSincronizador.iniciar(appScope)
        // PDF viejos de la caché: ningún formulario los usa al arrancar.
        appScope.launch(Dispatchers.IO) { limpiarPdfsTemporales(cacheDir) }
    }
}
