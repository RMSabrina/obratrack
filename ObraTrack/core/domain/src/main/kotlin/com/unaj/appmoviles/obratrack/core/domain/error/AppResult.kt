package com.unaj.appmoviles.obratrack.core.domain.error

sealed interface AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>
    data class Error(val error: DomainError) : AppResult<Nothing>
}