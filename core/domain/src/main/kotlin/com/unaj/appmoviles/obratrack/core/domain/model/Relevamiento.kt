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
    /** Minutos desde la medianoche (0..1439) de la próxima visita; null = sin hora. */
    val horaVisita: Int? = null,
    /** Tarea vinculada (opcional). Los relevamientos anteriores a la Etapa 4 no la tienen. */
    val tareaId: String? = null
)