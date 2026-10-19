package com.unaj.appmoviles.obratrack

import AppContainer
import android.app.Application
import com.unaj.appmoviles.obratrack.notification.NotificationHelper
import com.unaj.appmoviles.obratrack.worker.RelevamientoReminderWorker

class ObraTrackApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer()
        NotificationHelper.createNotificationChannels(this)
        RelevamientoReminderWorker.schedulePeriodic(this)
    }
}
