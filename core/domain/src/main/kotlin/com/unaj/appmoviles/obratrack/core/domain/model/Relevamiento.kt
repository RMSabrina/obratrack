package com.unaj.appmoviles.obratrack.core.domain.model

data class Relevamiento(
    val id: String,
    val obraId: String,
    val userId: String,
    val fecha: Long,
    val nota: String,
    val fotoUrl: String? = null,
    val ubicacion: Ubicacion? = null,
    val proximaVisita: Long? = null,
    /** Tarea vinculada (opcional). Los relevamientos anteriores a la Etapa 4 no la tienen. */
    val tareaId: String? = null
)