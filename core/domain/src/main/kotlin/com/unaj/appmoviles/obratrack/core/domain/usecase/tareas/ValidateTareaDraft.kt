package com.unaj.appmoviles.obratrack.core.domain.usecase.tareas

import com.unaj.appmoviles.obratrack.core.domain.error.TareaError
import com.unaj.appmoviles.obratrack.core.domain.model.TareaDraft

internal fun TareaDraft.normalized(): TareaDraft =
    copy(titulo = titulo.trim(), descripcion = descripcion.trim())

internal fun TareaDraft.validationError(): TareaError? = when {
    titulo.isBlank() -> TareaError.TituloVacio
    (fechaInicio == null) != (fechaFin == null) -> TareaError.FechasIncompletas
    fechaInicio != null && fechaFin != null && fechaFin.isBefore(fechaInicio) ->
        TareaError.FechaFinAnterior
    else -> null
}
