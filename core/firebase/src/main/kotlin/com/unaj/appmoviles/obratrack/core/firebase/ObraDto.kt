package com.unaj.appmoviles.obratrack.core.firebase

/**
 * Documento de Firestore. No tiene lógica de negocio.
 * Las fechas viajan en milisegundos; el data source las convierte a Timestamp.
 */
data class ObraDto(
    val id: String,
    val userId: String,
    val nombre: String,
    val cliente: String,
    val publica: Boolean,
    val direccion: String,
    val fechaInicioMillis: Long,
    val estado: String,
    val ultimaVisitaMillis: Long?,
    val latitud: Double? = null,
    val longitud: Double? = null
)
