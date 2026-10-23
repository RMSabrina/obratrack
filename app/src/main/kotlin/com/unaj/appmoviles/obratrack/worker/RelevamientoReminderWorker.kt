package com.unaj.appmoviles.obratrack.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.hilt.work.HiltWorker
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import com.unaj.appmoviles.obratrack.core.domain.repository.AuthRepository
import com.unaj.appmoviles.obratrack.core.domain.repository.ObraRepository
import com.unaj.appmoviles.obratrack.core.domain.model.Obra
import com.unaj.appmoviles.obratrack.core.domain.model.EstadoObra
import com.unaj.appmoviles.obratrack.notification.NotificationHelper
import kotlinx.coroutines.flow.firstOrNull
import java.util.concurrent.TimeUnit

@HiltWorker
class RelevamientoReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val authRepository: AuthRepository,
    private val obraRepository: ObraRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        NotificationHelper.createNotificationChannels(applicationContext)

        val sessionUser = authRepository.session.firstOrNull() ?: return Result.success()
        val obras: List<Obra> = obraRepository.observe(sessionUser.id).firstOrNull().orEmpty()

        val now = System.currentTimeMillis()
        val oneDayMillis = 24L * 60 * 60 * 1000L
        val sevenDaysMillis = 7L * 24L * 60 * 60 * 1000L

        var notifiedCount = 0
        for (obra in obras) {
            val proxima = obra.proximaVisita
            if (proxima != null) {
                val diff = proxima - now
                if (diff < oneDayMillis) {
                    val title = if (diff < 0) "⚠️ Visita atrasada: ${obra.nombre}" else "📅 Visita técnica hoy: ${obra.nombre}"
                    val msg = if (diff < 0) {
                        "La visita planificada para '${obra.nombre}' está pendiente de relevamiento."
                    } else {
                        "Tenés inspección técnica programada para las próximas horas en '${obra.nombre}'."
                    }
                    NotificationHelper.showNotification(
                        applicationContext,
                        obra.id.hashCode(),
                        title,
                        msg
                    )
                    notifiedCount++
                    continue
                }
            }

            // Alerta por inactividad de obra activa (RF7: obra activa con más de X días sin relevamiento)
            if (obra.estado == EstadoObra.ACTIVA) {
                val ultima = obra.ultimaVisita ?: obra.fechaInicio
                val diffSinVisita = now - ultima
                if (diffSinVisita >= sevenDaysMillis) {
                    val diasSinVisita = diffSinVisita / oneDayMillis
                    val title = "⚠️ Obra sin relevamientos: ${obra.nombre}"
                    val msg = "La obra '${obra.nombre}' para '${obra.cliente}' lleva $diasSinVisita días sin visitas registradas."
                    NotificationHelper.showNotification(
                        applicationContext,
                        obra.id.hashCode() + 1,
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
