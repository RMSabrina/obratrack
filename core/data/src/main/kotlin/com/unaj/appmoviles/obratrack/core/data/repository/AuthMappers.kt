package com.unaj.appmoviles.obratrack.core.data.repository

import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.unaj.appmoviles.obratrack.core.domain.model.User
import com.unaj.appmoviles.obratrack.core.domain.error.AppResult
import com.unaj.appmoviles.obratrack.core.domain.error.AuthError
import com.unaj.appmoviles.obratrack.core.firebase.FirebaseResult
import com.unaj.appmoviles.obratrack.core.firebase.UserDto

fun UserDto.toDomain(): User {
    return User(
        id = this.uid,
        email = this.email,
        name = this.displayName ?: ""
    )
}

fun <T, R> FirebaseResult<T>.toAppResult(mapData: (T) -> R): AppResult<R> {
    return when (this) {
        is FirebaseResult.Success -> AppResult.Success(mapData(this.data))
        is FirebaseResult.Error -> {
            val domainError = when (this.exception) {
                is FirebaseAuthInvalidCredentialsException -> AuthError.InvalidCredentials
                is FirebaseAuthUserCollisionException -> AuthError.EmailAlreadyInUse
                is com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException -> AuthError.RequiresRecentLogin
                // Firebase lanza FirebaseNetworkException cuando no hay internet
                is com.google.firebase.FirebaseNetworkException -> AuthError.NetworkError
                else -> when (this.exception.message) {
                    "EMAIL_NOT_VERIFIED" -> AuthError.EmailNotVerified
                    "EMAIL_ALREADY_VERIFIED" -> AuthError.EmailAlreadyVerified
                    else -> AuthError.Unknown
                }
            }
            AppResult.Error(domainError)
        }
    }
}