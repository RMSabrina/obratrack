package com.unaj.appmoviles.obratrack.core.domain.model

data class RelevamientoDraft(
    val fecha: Long,
    val nota: String,
    val fotoUrl: String? = null,
    val ubicacion: Ubicacion? = null,
    val proximaVisita: Long? = null,
    /** null = sin tarea vinculada. */
    val tareaId: String? = null
)