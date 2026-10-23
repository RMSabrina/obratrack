package com.unaj.appmoviles.obratrack.core.domain.usecase.relevamientos

import com.unaj.appmoviles.obratrack.core.domain.error.RelevamientoError
import com.unaj.appmoviles.obratrack.core.domain.model.RelevamientoDraft
import com.unaj.appmoviles.obratrack.core.domain.model.TipoRelevamiento

internal fun RelevamientoDraft.normalized(): RelevamientoDraft = copy(
    nota = nota.trim(),
    tipo = tipo.ifBlank { TipoRelevamiento.AVANCE }
)

internal fun RelevamientoDraft.validationError(): RelevamientoError? = when {
    fecha <= 0L -> RelevamientoError.FechaInvalida
    nota.isBlank() -> RelevamientoError.NotaVacia
    else -> null
}
