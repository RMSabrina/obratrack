package com.unaj.appmoviles.obratrack.core.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Ids de los documentos con cambios guardados en el teléfono que el servidor todavía no confirmó.
 * Es un flujo aparte de los listados para que quienes no lo usan (Agenda, avisos) no se recalculen
 * cuando el servidor confirma. Si no se puede leer, emite un conjunto vacío.
 */
interface PendientesRepository {
    fun obras(userId: String): Flow<Set<String>>
    fun relevamientos(obraId: String): Flow<Set<String>>
    fun tareas(obraId: String): Flow<Set<String>>
}
