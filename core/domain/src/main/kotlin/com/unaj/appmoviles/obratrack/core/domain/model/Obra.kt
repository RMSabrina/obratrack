package com.unaj.appmoviles.obratrack.core.domain.model

data class Obra(
    val id: String,
    val userId: String,
    val nombre: String,
    val cliente: String,
    val direccion: String,
    val publica: Boolean,
    val estado: EstadoObra,
    val fechaInicio: Long,
    val ultimaVisita: Long?,
    val ubicacion: Ubicacion? = null
)
