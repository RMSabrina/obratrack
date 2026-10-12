package com.unaj.appmoviles.obratrack.core.domain.error

sealed interface ObraError : DomainError {
    data object NombreVacio : ObraError
    data object ClienteVacio : ObraError
    data object DireccionVacia : ObraError
    data object NoEncontrada : ObraError
    data object NoAutorizada : ObraError
    data object Red : ObraError
    data object Desconocido : ObraError
}
