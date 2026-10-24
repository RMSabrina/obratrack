package com.unaj.appmoviles.obratrack.core.domain.error

sealed interface TareaError : DomainError {
    data object TituloVacio : TareaError
    data object FechasIncompletas : TareaError
    data object FechaFinAnterior : TareaError
    data object NoEncontrada : TareaError
    data object NoAutorizada : TareaError
    data object Red : TareaError
    data object Timeout : TareaError
    /** Más relevamientos vinculados de los que se pueden procesar en un solo batch. */
    data object DemasiadosVinculados : TareaError
    data object Desconocido : TareaError
}
