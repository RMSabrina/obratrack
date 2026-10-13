package com.unaj.appmoviles.obratrack.core.firebase

/**
 * Documento de la subcolección obras/{obraId}/relevamientos.
 * La fecha viaja en milisegundos; el data source la convierte a Timestamp.
 */
data class RelevamientoDto(
    val id: String,
    val obraId: String,
    val userId: String,
    val fechaMillis: Long,
    val nota: String
)
