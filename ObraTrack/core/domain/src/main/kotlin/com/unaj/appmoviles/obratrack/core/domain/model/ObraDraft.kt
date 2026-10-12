package com.unaj.appmoviles.obratrack.core.domain.model

data class ObraDraft(
    val nombre: String,
    val cliente: String,
    val direccion: String,
    val publica: Boolean,
    val estado: EstadoObra
)
