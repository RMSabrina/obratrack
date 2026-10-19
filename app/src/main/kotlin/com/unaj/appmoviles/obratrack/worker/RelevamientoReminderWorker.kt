package com.unaj.appmoviles.obratrack.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.unaj.appmoviles.obratrack.ObraTrackApplication
import com.unaj.appmoviles.obratrack.core.domain.model.Obra
import com.unaj.appmoviles.obratrack.notification.NotificationHelper
import kotlinx.coroutines.flow.firstOrNull
import java.util.concurrent.TimeUnit

class RelevamientoReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        NotificationHelper.createNotificationChannels(applicationContext)

        val app = applicationContext as? ObraTrackApplication ?: return Result.success()
        val sessionUser = app.container.authRepository.session.firstOrNull() ?: return Result.success()
        val obras: List<Obra> = app.container.obraRepository.observe(sessionUser.id).firstOrNull().orEmpty()

        val now = System.currentTimeMillis()
        val oneDayMillis = 24L * 60 * 60 * 1000

        var notifiedCount = 0
        for (obra in obras) {
            val proxima = obra.proximaVisita
            if (proxima != null) {
                val diff = proxima - now
                if (diff < oneDayMillis) {
                    val title = if (diff < 0) "⚠️ Visita atrasada: ${obra.nombre}" else "📅 Visita técnica hoy: ${obra.nombre}"
                    val msg = if (diff < 0) {
                        "La visita planificada para ${obra.cliente} está pendiente de relevamiento."
                    } else {
                        "Tenés inspección técnica programada para las próximas horas en ${obra.direccion}."
                    }
                    NotificationHelper.showNotification(
                        applicationContext,
                        obra.id.hashCode(),
                        title,
                        msg
                    )
                    notifiedCount++
                }
            }
        }

        if (notifiedCount == 0 && isImmediateDemo) {
            NotificationHelper.showDemoNotification(applicationContext)
        }

        return Result.success()
    }

    companion object {
        private const val PERIODIC_WORK_NAME = "relevamiento_periodic_reminders"
        var isImmediateDemo: Boolean = false

        fun schedulePeriodic(context: Context) {
            val periodicRequest = PeriodicWorkRequestBuilder<RelevamientoReminderWorker>(
                12, TimeUnit.HOURS
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicRequest
            )
        }

        fun triggerImmediateDemo(context: Context) {
            isImmediateDemo = true
            val oneTimeRequest = OneTimeWorkRequestBuilder<RelevamientoReminderWorker>().build()
            WorkManager.getInstance(context).enqueue(oneTimeRequest)
        }
    }
}
