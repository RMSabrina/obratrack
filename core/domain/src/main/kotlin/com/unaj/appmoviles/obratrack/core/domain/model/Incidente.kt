package com.unaj.appmoviles.obratrack.core.domain.model

enum class GravedadIncidente {
    BAJA,
    MEDIA,
    ALTA
}

data class Incidente(
    val id: String,
    val obraId: String,
    val titulo: String,
    val descripcion: String,
    val gravedad: GravedadIncidente,
    val reportadoPorUserId: String,
    val reportadoPorNombre: String,
    val fecha: Long,
    val resuelto: Boolean = false,
    val fotoUrl: String? = null
)
