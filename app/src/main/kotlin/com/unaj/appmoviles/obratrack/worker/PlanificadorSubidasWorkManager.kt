package com.unaj.appmoviles.obratrack.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.unaj.appmoviles.obratrack.core.data.fotos.PlanificadorSubidas
import com.unaj.appmoviles.obratrack.core.data.fotos.SubidaProgramada
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Una subida única por relevamiento, solo con red y con reintentos de espera creciente. */
@Singleton
class PlanificadorSubidasWorkManager @Inject constructor(
    @ApplicationContext context: Context
) : PlanificadorSubidas {

    // Perezoso: WorkManager se inicializa con la Application ya inyectada (su fábrica de workers).
    private val workManager by lazy { WorkManager.getInstance(context) }

    override val programadas: Flow<Set<SubidaProgramada>> =
        flow { emitAll(workManager.getWorkInfosByTagFlow(TAG_FOTO)) }
            .map { trabajos ->
                trabajos
                    .filter { !it.state.isFinished }
                    .mapNotNull { it.aSubida() }
                    .toSet()
            }
            .distinctUntilChanged()

    override fun programar(obraId: String, relevamientoId: String) {
        val pedido = OneTimeWorkRequestBuilder<SubirFotoWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_INICIAL_SEGUNDOS, TimeUnit.SECONDS)
            .setInputData(
                workDataOf(
                    SubirFotoWorker.KEY_OBRA_ID to obraId,
                    SubirFotoWorker.KEY_RELEVAMIENTO_ID to relevamientoId
                )
            )
            .addTag(TAG_FOTO)
            .addTag(tagObra(obraId))
            .addTag(tagRelevamiento(relevamientoId))
            .build()
        // REPLACE: una foto nueva para el mismo relevamiento reemplaza la subida que estaba en curso.
        workManager.enqueueUniqueWork(nombreTrabajo(relevamientoId), ExistingWorkPolicy.REPLACE, pedido)
    }

    override fun cancelar(relevamientoId: String) {
        workManager.cancelUniqueWork(nombreTrabajo(relevamientoId))
    }

    override fun cancelarDeObra(obraId: String) {
        workManager.cancelAllWorkByTag(tagObra(obraId))
    }

    private fun WorkInfo.aSubida(): SubidaProgramada? {
        val obraId = tags.firstOrNull { it.startsWith(PREFIJO_OBRA) }?.removePrefix(PREFIJO_OBRA)
        val relevamientoId = tags.firstOrNull { it.startsWith(PREFIJO_RELEVAMIENTO) }?.removePrefix(PREFIJO_RELEVAMIENTO)
        return if (obraId != null && relevamientoId != null) SubidaProgramada(obraId, relevamientoId) else null
    }

    private companion object {
        const val TAG_FOTO = "foto_pendiente"
        const val PREFIJO_OBRA = "foto_obra:"
        const val PREFIJO_RELEVAMIENTO = "foto_relevamiento:"
        const val BACKOFF_INICIAL_SEGUNDOS = 30L

        fun tagObra(obraId: String) = PREFIJO_OBRA + obraId
        fun tagRelevamiento(relevamientoId: String) = PREFIJO_RELEVAMIENTO + relevamientoId
        fun nombreTrabajo(relevamientoId: String) = "subir_foto_$relevamientoId"
    }
}
