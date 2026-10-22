package com.unaj.appmoviles.obratrack.core.domain.model

import java.time.LocalDate

data class Tarea(
    val id: String,
    val obraId: String,
    val userId: String,
    val titulo: String,
    val descripcion: String = "",
    val categoria: CategoriaTarea = CategoriaTarea.OTROS,
    val fechaInicio: LocalDate? = null,
    val fechaFin: LocalDate? = null,
    val estado: EstadoTarea = EstadoTarea.PENDIENTE
)
