package com.unaj.appmoviles.obratrack.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.unaj.appmoviles.features.obras.formatFecha
import com.unaj.appmoviles.features.obras.formatHora
import com.unaj.appmoviles.obratrack.R
import com.unaj.appmoviles.obratrack.core.domain.repository.AuthRepository
import com.unaj.appmoviles.obratrack.core.domain.repository.LanguagePreferencesRepository
import com.unaj.appmoviles.obratrack.core.domain.repository.ObraRepository
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.toFechaCalendario
import com.unaj.appmoviles.obratrack.notification.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.firstOrNull
import java.time.Clock
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Aviso de una visita concreta, programado para su instante (visita menos la anticipación elegida). */
@HiltWorker
class AvisoVisitaWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val authRepository: AuthRepository,
    private val obraRepository: ObraRepository,
    private val languagePreferences: LanguagePreferencesRepository,
    private val clock: Clock
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val obraId = inputData.getString(KEY_OBRA_ID) ?: return Result.failure()
        val proximaVisita = inputData.getLong(KEY_PROXIMA_VISITA, -1L)
        val hora = inputData.getInt(KEY_HORA, SIN_HORA).takeIf { it != SIN_HORA }

        val usuario = authRepository.session.firstOrNull() ?: return Result.success()
        val obra = obraRepository.observe(usuario.id).firstOrNull().orEmpty().firstOrNull { it.id == obraId }
            ?: return Result.success()
        // Si la visita cambió o se quitó después de programar este aviso, ya no corresponde.
        if (obra.proximaVisita != proximaVisita || obra.horaVisita != hora) return Result.success()

        // El texto sale en el idioma elegido en la app, no en el del sistema.
        val contexto = NotificationHelper.contextoLocalizado(applicationContext, languagePreferences.getLanguage())
        val fecha = proximaVisita.toFechaCalendario()
        val horaTexto = hora?.let { formatHora(contexto, it) }
        val mensaje = when (ChronoUnit.DAYS.between(LocalDate.now(clock), fecha)) {
            0L ->
                if (horaTexto != null) contexto.getString(R.string.notif_aviso_hoy_con_hora, horaTexto, obra.direccion)
                else contexto.getString(R.string.notif_aviso_hoy_sin_hora, obra.direccion)
            1L ->
                if (horaTexto != null) contexto.getString(R.string.notif_aviso_manana_con_hora, horaTexto, obra.direccion)
                else contexto.getString(R.string.notif_aviso_manana_sin_hora, obra.direccion)
            else ->
                if (horaTexto != null) contexto.getString(R.string.notif_aviso_otro_dia_con_hora, formatFecha(fecha), horaTexto, obra.direccion)
                else contexto.getString(R.string.notif_aviso_otro_dia_sin_hora, formatFecha(fecha), obra.direccion)
        }

        NotificationHelper.createNotificationChannels(applicationContext)
        NotificationHelper.showNotification(
            applicationContext,
            obra.id.hashCode() + 2,
            contexto.getString(R.string.notif_aviso_titulo, obra.nombre),
            mensaje,
            obraId = obra.id
        )
        return Result.success()
    }

    companion object {
        const val KEY_OBRA_ID = "obra_id"
        const val KEY_PROXIMA_VISITA = "proxima_visita"
        const val KEY_HORA = "hora_visita"
        const val SIN_HORA = -1

        /** Un solo trabajo por visita: reprogramar con este nombre reemplaza el anterior. */
        fun nombreTrabajo(obraId: String) = "aviso_visita_$obraId"
    }
}
