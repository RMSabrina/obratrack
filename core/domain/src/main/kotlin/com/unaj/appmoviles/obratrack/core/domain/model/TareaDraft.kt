package com.unaj.appmoviles.obratrack.core.domain.model

import java.time.LocalDate

data class TareaDraft(
    val titulo: String,
    val descripcion: String = "",
    val categoria: CategoriaTarea = CategoriaTarea.OTROS,
    val fechaInicio: LocalDate? = null,
    val fechaFin: LocalDate? = null,
    val estado: EstadoTarea = EstadoTarea.PENDIENTE
)
