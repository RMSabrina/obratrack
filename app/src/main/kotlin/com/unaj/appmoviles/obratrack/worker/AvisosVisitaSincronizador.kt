package com.unaj.appmoviles.obratrack.worker

import android.content.Context
import android.util.Log
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.unaj.appmoviles.obratrack.core.domain.model.AnticipacionAviso
import com.unaj.appmoviles.obratrack.core.domain.model.Obra
import com.unaj.appmoviles.obratrack.core.domain.repository.AuthRepository
import com.unaj.appmoviles.obratrack.core.domain.repository.AvisoPreferencesRepository
import com.unaj.appmoviles.obratrack.core.domain.repository.ObraRepository
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.PlanAviso
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.instanteVisita
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.planificarAviso
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.toFechaCalendario
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Duration
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mantiene un aviso programado por cada obra con próxima visita. Mira las obras del usuario y la
 * anticipación elegida: cuando cambia la fecha, la hora, la anticipación o la zona horaria cancela
 * el trabajo anterior y programa el nuevo. Recuerda qué programó para no repetir avisos.
 */
@Singleton
class AvisosVisitaSincronizador @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authRepository: AuthRepository,
    private val obraRepository: ObraRepository,
    private val avisoPreferences: AvisoPreferencesRepository,
    private val clock: Clock
) {
    /** obraId -> firma de lo último que se programó. */
    private val programados = context.getSharedPreferences("obratrack_avisos_programados", Context.MODE_PRIVATE)

    @OptIn(ExperimentalCoroutinesApi::class)
    fun iniciar(scope: CoroutineScope) {
        scope.launch {
            authRepository.session
                .flatMapLatest { usuario ->
                    if (usuario == null) emptyFlow() else obraRepository.observe(usuario.id)
                }
                .combine(avisoPreferences.observeAnticipacion()) { obras, anticipacion -> obras to anticipacion }
                .catch { Log.w(TAG, "No se pudieron sincronizar los avisos de visita", it) }
                .collect { (obras, anticipacion) -> sincronizar(obras, anticipacion) }
        }
    }

    private fun sincronizar(obras: List<Obra>, anticipacion: AnticipacionAviso) {
        val workManager = WorkManager.getInstance(context)
        val ahora = clock.instant()
        for (obra in obras) {
            val nombre = AvisoVisitaWorker.nombreTrabajo(obra.id)
            val proxima = obra.proximaVisita
            if (proxima == null) {
                if (programados.contains(obra.id)) {
                    workManager.cancelUniqueWork(nombre)
                    programados.edit().remove(obra.id).apply()
                }
                continue
            }

            val firma = "$proxima|${obra.horaVisita}|${anticipacion.name}|${clock.zone.id}"
            if (programados.getString(obra.id, null) == firma) continue

            val visita = instanteVisita(proxima.toFechaCalendario(), obra.horaVisita, clock.zone)
            when (val plan = planificarAviso(ahora, visita, anticipacion)) {
                is PlanAviso.Programar -> encolar(workManager, obra, proxima, Duration.between(ahora, plan.instante))
                PlanAviso.AvisarYa -> encolar(workManager, obra, proxima, Duration.ZERO)
                PlanAviso.Ninguno -> workManager.cancelUniqueWork(nombre)
            }
            programados.edit().putString(obra.id, firma).apply()
        }
    }

    private fun encolar(workManager: WorkManager, obra: Obra, proxima: Long, demora: Duration) {
        val datos = Data.Builder()
            .putString(AvisoVisitaWorker.KEY_OBRA_ID, obra.id)
            .putLong(AvisoVisitaWorker.KEY_PROXIMA_VISITA, proxima)
            .putInt(AvisoVisitaWorker.KEY_HORA, obra.horaVisita ?: AvisoVisitaWorker.SIN_HORA)
            .build()
        val pedido = OneTimeWorkRequestBuilder<AvisoVisitaWorker>()
            .setInitialDelay(demora.toMillis(), TimeUnit.MILLISECONDS)
            .setInputData(datos)
            .build()
        // REPLACE: cancela el aviso anterior de esta visita y deja solo el nuevo.
        workManager.enqueueUniqueWork(AvisoVisitaWorker.nombreTrabajo(obra.id), ExistingWorkPolicy.REPLACE, pedido)
    }

    private companion object {
        const val TAG = "AvisosVisita"
    }
}
