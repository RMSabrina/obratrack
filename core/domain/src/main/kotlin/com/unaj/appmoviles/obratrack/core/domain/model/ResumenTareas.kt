package com.unaj.appmoviles.obratrack.core.domain.model

/**
 * Conteo de tareas de una obra. [pendientes] son todas las no completadas (con o sin
 * fechas); [atrasadas] es un subconjunto de [pendientes] que se resalta aparte.
 */
data class ResumenTareas(
    val completadas: Int,
    val pendientes: Int,
    val atrasadas: Int
)
