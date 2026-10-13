package com.unaj.appmoviles.obratrack.core.domain.model

data class Relevamiento(
    val id: String,
    val obraId: String,
    val userId: String,
    val fecha: Long,
    val nota: String
)
