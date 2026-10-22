package com.unaj.appmoviles.obratrack.core.firebase

/**
 * Documento de la subcolección obras/{obraId}/tareas.
 * Las fechas viajan como string ISO ("2026-10-21"); categoria y estado, como el nombre del enum.
 */
data class TareaDto(
    val id: String,
    val obraId: String,
    val userId: String,
    val titulo: String,
    val descripcion: String = "",
    val categoria: String,
    val estado: String,
    val fechaInicio: String? = null,
    val fechaFin: String? = null
)
