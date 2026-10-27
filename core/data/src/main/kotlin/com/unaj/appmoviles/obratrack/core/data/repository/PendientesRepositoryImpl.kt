package com.unaj.appmoviles.obratrack.core.data.repository

import android.util.Log
import com.unaj.appmoviles.obratrack.core.domain.repository.PendientesRepository
import com.unaj.appmoviles.obratrack.core.firebase.FirestorePendientesDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import javax.inject.Inject
import javax.inject.Singleton

/** El indicador es accesorio: si no se puede leer, no hay marcas, pero nunca se rompe la pantalla. */
@Singleton
class PendientesRepositoryImpl @Inject constructor(
    private val dataSource: FirestorePendientesDataSource
) : PendientesRepository {

    override fun obras(userId: String) = dataSource.obras(userId).sinFallar()
    override fun relevamientos(obraId: String) = dataSource.relevamientos(obraId).sinFallar()
    override fun tareas(obraId: String) = dataSource.tareas(obraId).sinFallar()

    private fun Flow<Set<String>>.sinFallar(): Flow<Set<String>> = catch { e ->
        Log.w("PendientesRepository", "No se pudo leer el estado de sincronización", e)
        emit(emptySet())
    }
}
