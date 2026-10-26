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
    val ubicacion: Ubicacion? = null,
    val proximaVisita: Long? = null,
    /** Minutos desde la medianoche (0..1439) de [proximaVisita]; null = sin hora. */
    val horaVisita: Int? = null
)
