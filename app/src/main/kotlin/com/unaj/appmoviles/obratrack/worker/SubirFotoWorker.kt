package com.unaj.appmoviles.obratrack.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.unaj.appmoviles.obratrack.core.data.fotos.ResultadoSubida
import com.unaj.appmoviles.obratrack.core.data.fotos.SubidorFotosPendientes
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/** Sube la foto guardada de un relevamiento y se la asigna. Corre solo con red (ver el planificador). */
@HiltWorker
class SubirFotoWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val subidor: SubidorFotosPendientes
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val obraId = inputData.getString(KEY_OBRA_ID) ?: return Result.failure()
        val relevamientoId = inputData.getString(KEY_RELEVAMIENTO_ID) ?: return Result.failure()
        return when (subidor.subir(obraId, relevamientoId, intento = runAttemptCount)) {
            ResultadoSubida.REINTENTAR -> Result.retry()
            ResultadoSubida.LISTO, ResultadoSubida.DESCARTADA -> Result.success()
        }
    }

    companion object {
        const val KEY_OBRA_ID = "obra_id"
        const val KEY_RELEVAMIENTO_ID = "relevamiento_id"
    }
}
