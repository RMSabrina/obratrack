package com.unaj.appmoviles.obratrack.core.domain.usecase.auth

import com.unaj.appmoviles.obratrack.core.domain.error.AppResult
import com.unaj.appmoviles.obratrack.core.domain.repository.AuthRepository
import javax.inject.Inject

class ResendVerificationUseCase @Inject constructor(private val repository: AuthRepository) {
    suspend operator fun invoke(email: String, password: String): AppResult<Unit> =
        repository.resendVerificationEmail(email, password)
}