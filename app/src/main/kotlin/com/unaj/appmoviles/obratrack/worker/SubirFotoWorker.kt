package com.unaj.appmoviles.obratrack.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.unaj.appmoviles.obratrack.R
import com.unaj.appmoviles.obratrack.core.data.fotos.ResultadoSubida
import com.unaj.appmoviles.obratrack.core.data.fotos.SubidorFotosPendientes
import com.unaj.appmoviles.obratrack.core.domain.repository.LanguagePreferencesRepository
import com.unaj.appmoviles.obratrack.notification.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/** Sube la foto guardada de un relevamiento y se la asigna. Corre solo con red (ver el planificador). */
@HiltWorker
class SubirFotoWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val subidor: SubidorFotosPendientes,
    private val languagePreferences: LanguagePreferencesRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val obraId = inputData.getString(KEY_OBRA_ID) ?: return Result.failure()
        val relevamientoId = inputData.getString(KEY_RELEVAMIENTO_ID) ?: return Result.failure()
        return when (subidor.subir(obraId, relevamientoId, intento = runAttemptCount)) {
            ResultadoSubida.REINTENTAR -> Result.retry()
            ResultadoSubida.PERDIDA -> {
                avisarFotoPerdida(obraId, relevamientoId)
                Result.success()
            }
            ResultadoSubida.LISTO, ResultadoSubida.DESCARTADA -> Result.success()
        }
    }

    // La app puede estar cerrada: el aviso es una notificación, en el idioma elegido en la app.
    private fun avisarFotoPerdida(obraId: String, relevamientoId: String) {
        val contexto = NotificationHelper.contextoLocalizado(applicationContext, languagePreferences.getBackgroundLanguage())
        NotificationHelper.createNotificationChannels(applicationContext)
        NotificationHelper.showNotification(
            applicationContext,
            relevamientoId.hashCode() + 3,
            contexto.getString(R.string.notif_foto_perdida_titulo),
            contexto.getString(R.string.notif_foto_perdida_mensaje),
            obraId = obraId
        )
    }

    companion object {
        const val KEY_OBRA_ID = "obra_id"
        const val KEY_RELEVAMIENTO_ID = "relevamiento_id"
    }
}
