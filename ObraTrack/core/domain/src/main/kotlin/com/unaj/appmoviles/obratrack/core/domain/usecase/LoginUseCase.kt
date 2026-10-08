package com.unaj.appmoviles.obratrack.core.domain.usecase

import com.unaj.appmoviles.obratrack.core.domain.error.AppResult
import com.unaj.appmoviles.obratrack.core.domain.error.AuthError
import com.unaj.appmoviles.obratrack.core.domain.model.Credentials
import com.unaj.appmoviles.obratrack.core.domain.repository.AuthRepository

class LoginUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(credentials: Credentials): AppResult<Unit> {
        if (credentials.email.isBlank() || !credentials.email.contains("@")) {
            return AppResult.Error(AuthError.InvalidEmail)
        }

        if (credentials.password.length < 6) {
            return AppResult.Error(AuthError.PasswordTooShort)
        }

        return authRepository.login(credentials)
    }
}