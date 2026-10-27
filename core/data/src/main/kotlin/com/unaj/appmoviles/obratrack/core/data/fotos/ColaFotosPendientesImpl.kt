package com.unaj.appmoviles.obratrack.core.data.fotos

import com.unaj.appmoviles.obratrack.core.domain.repository.ColaFotosPendientes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ColaFotosPendientesImpl @Inject constructor(
    private val store: FotosPendientesStore,
    private val planificador: PlanificadorSubidas
) : ColaFotosPendientes {

    // Solo cuentan las subidas cuyo archivo sigue en disco (si se descartó, ya no hay nada que mostrar).
    override val pendientes: Flow<Map<String, String>> = planificador.programadas
        .map { subidas ->
            subidas.mapNotNull { subida ->
                store.archivo(subida.obraId, subida.relevamientoId)
                    ?.let { subida.relevamientoId to it.absolutePath }
            }.toMap()
        }
        .distinctUntilChanged()

    override suspend fun encolar(obraId: String, relevamientoId: String, bytes: ByteArray) {
        withContext(Dispatchers.IO) { store.guardar(obraId, relevamientoId, bytes) }
        planificador.programar(obraId, relevamientoId)
    }

    override suspend fun cancelar(obraId: String, relevamientoId: String) {
        planificador.cancelar(relevamientoId)
        withContext(Dispatchers.IO) { store.borrar(obraId, relevamientoId) }
    }

    override suspend fun cancelarDeObra(obraId: String) {
        planificador.cancelarDeObra(obraId)
        withContext(Dispatchers.IO) { store.borrarObra(obraId) }
    }
}
