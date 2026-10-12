package com.unaj.appmoviles.obratrack.core.domain.usecase

import com.unaj.appmoviles.obratrack.core.domain.error.ObraError
import com.unaj.appmoviles.obratrack.core.domain.model.ObraDraft

internal fun ObraDraft.normalized(): ObraDraft = copy(
    nombre = nombre.trim(),
    cliente = cliente.trim(),
    direccion = direccion.trim()
)

internal fun ObraDraft.validationError(): ObraError? = when {
    nombre.isBlank() -> ObraError.NombreVacio
    cliente.isBlank() -> ObraError.ClienteVacio
    direccion.isBlank() -> ObraError.DireccionVacia
    else -> null
}
