package com.unaj.appmoviles.obratrack.core.domain.usecase.auth

import com.unaj.appmoviles.obratrack.core.domain.error.AppResult
import com.unaj.appmoviles.obratrack.core.domain.error.AuthError
import com.unaj.appmoviles.obratrack.core.domain.model.User
import com.unaj.appmoviles.obratrack.core.domain.repository.AuthRepository

class UpdateProfileNameUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(newName: String): AppResult<User> {
        val trimmed = newName.trim()
        if (trimmed.isBlank()) {
            return AppResult.Error(AuthError.BlankName)
        }
        return authRepository.updateProfileName(trimmed)
    }
}
