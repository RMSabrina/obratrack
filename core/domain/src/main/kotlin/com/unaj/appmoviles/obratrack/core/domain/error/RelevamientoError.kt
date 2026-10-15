package com.unaj.appmoviles.obratrack.core.domain.error

sealed interface RelevamientoError : DomainError {
    data object NotaVacia : RelevamientoError
    data object FechaInvalida : RelevamientoError
    data object NoEncontrado : RelevamientoError
    data object NoAutorizado : RelevamientoError
    data object ImagenMuyGrande : RelevamientoError
    data object Red : RelevamientoError
    data object Desconocido : RelevamientoError
}
