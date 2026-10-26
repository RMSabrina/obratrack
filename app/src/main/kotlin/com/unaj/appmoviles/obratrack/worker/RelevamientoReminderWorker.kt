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
import com.unaj.appmoviles.obratrack.R
import com.unaj.appmoviles.obratrack.notification.NotificationHelper
import kotlinx.coroutines.flow.firstOrNull
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.EstadoFechaVisita
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.EstadoVisitaConHora
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.estadoFechaVisita
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.estadoVisitaConHora
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.toFechaCalendario
import java.time.Clock
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

@HiltWorker
class RelevamientoReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val authRepository: AuthRepository,
    private val obraRepository: ObraRepository,
    private val clock: Clock
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        NotificationHelper.createNotificationChannels(applicationContext)

        val sessionUser = authRepository.session.firstOrNull() ?: return Result.success()
        val obras: List<Obra> = obraRepository.observe(sessionUser.id).firstOrNull().orEmpty()

        val hoy = LocalDate.now(clock)
        val diasInactividadMinimos = 7L

        var notifiedCount = 0
        for (obra in obras) {
            val proxima = obra.proximaVisita
            if (proxima != null) {
                val fecha = proxima.toFechaCalendario()
                val hora = obra.horaVisita
                // Con hora vence al pasar la hora; sin hora, al terminar el día.
                val atrasada = if (hora != null) {
                    estadoVisitaConHora(fecha, hora, clock) is EstadoVisitaConHora.Atrasada
                } else {
                    estadoFechaVisita(fecha, hoy) is EstadoFechaVisita.Atrasada
                }
                // El aviso previo a la visita lo da AvisoVisitaWorker; este solo informa las atrasadas.
                if (atrasada) {
                    NotificationHelper.showNotification(
                        applicationContext,
                        obra.id.hashCode(),
                        applicationContext.getString(R.string.notif_visita_atrasada_titulo, obra.nombre),
                        applicationContext.getString(R.string.notif_visita_atrasada_mensaje, obra.cliente)
                    )
                    notifiedCount++
                    continue
                }
            }

            // Alerta por inactividad de obra activa (RF7: obra activa con más de X días sin relevamiento)
            if (obra.estado == EstadoObra.ACTIVA) {
                val ultima = (obra.ultimaVisita ?: obra.fechaInicio).toFechaCalendario()
                val diasSinVisita = ChronoUnit.DAYS.between(ultima, hoy)
                if (diasSinVisita >= diasInactividadMinimos) {
                    val title = applicationContext.getString(R.string.notif_sin_relevamientos_titulo, obra.nombre)
                    val msg = applicationContext.resources.getQuantityString(
                        R.plurals.notif_sin_relevamientos_mensaje,
                        diasSinVisita.toInt(),
                        obra.cliente,
                        diasSinVisita
                    )
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