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
    val tipo: String = TipoRelevamiento.AVANCE,
    val checklist: List<String> = emptyList()
)
