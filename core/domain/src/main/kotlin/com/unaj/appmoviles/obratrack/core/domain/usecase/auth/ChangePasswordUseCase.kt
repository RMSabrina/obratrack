package com.unaj.appmoviles.obratrack.core.domain.usecase.auth

import com.unaj.appmoviles.obratrack.core.domain.error.AppResult
import com.unaj.appmoviles.obratrack.core.domain.error.AuthError
import com.unaj.appmoviles.obratrack.core.domain.repository.AuthRepository

class ChangePasswordUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(newPassword: String): AppResult<Unit> {
        if (newPassword.length < 6) {
            return AppResult.Error(AuthError.PasswordTooShort)
        }
        return authRepository.updatePassword(newPassword)
    }
}
