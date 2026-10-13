package com.unaj.appmoviles.obratrack.core.domain.model

data class RelevamientoDraft(
    val fecha: Long,
    val nota: String,
    val fotoUrl: String? = null,
    val ubicacion: Ubicacion? = null
)
