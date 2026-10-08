package com.unaj.appmoviles.obratrack.core.domain.error

sealed interface AuthError : DomainError {
    data object InvalidEmail : AuthError
    data object PasswordTooShort : AuthError
    data object InvalidCredentials : AuthError
    data object EmailAlreadyInUse : AuthError
    data object NetworkError : AuthError
    data object Unknown : AuthError
}