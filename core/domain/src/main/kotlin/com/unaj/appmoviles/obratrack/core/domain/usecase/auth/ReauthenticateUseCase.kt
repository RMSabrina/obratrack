package com.unaj.appmoviles.obratrack.core.domain.usecase.auth

import com.unaj.appmoviles.obratrack.core.domain.error.AppResult
import com.unaj.appmoviles.obratrack.core.domain.error.AuthError
import com.unaj.appmoviles.obratrack.core.domain.repository.AuthRepository

class ReauthenticateUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(password: String): AppResult<Unit> {
        if (password.isBlank()) {
            return AppResult.Error(AuthError.InvalidCredentials)
        }
        return authRepository.reauthenticate(password)
    }
}
