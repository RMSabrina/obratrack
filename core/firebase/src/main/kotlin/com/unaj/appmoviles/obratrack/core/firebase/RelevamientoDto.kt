package com.unaj.appmoviles.obratrack.core.firebase

/**
 * Documento de la subcolecciA3n obras/{obraId}/relevamientos.
 * La fecha viaja en milisegundos; el data source la convierte a Timestamp.
 */
data class RelevamientoDto(
    val id: String,
    val obraId: String,
    val userId: String,
    val fechaMillis: Long,
    val nota: String,
    val fotoUrl: String? = null,
    val latitud: Double? = null,
    val longitud: Double? = null,
    val proximaVisitaMillis: Long? = null,
    /** Campo opcional: los documentos anteriores a la Etapa 4 no lo tienen y se leen como null. */
    val tareaId: String? = null
)