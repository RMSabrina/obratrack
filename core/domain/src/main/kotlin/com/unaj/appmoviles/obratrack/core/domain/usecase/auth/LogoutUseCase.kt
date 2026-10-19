package com.unaj.appmoviles.obratrack.core.domain.usecase.auth

import com.unaj.appmoviles.obratrack.core.domain.repository.AuthRepository
import javax.inject.Inject

class LogoutUseCase @Inject constructor(
    private val authRepository: AuthRepository
    // Para el futuro: private val obrasRepository: ObrasRepository
) {
    suspend operator fun invoke() {
        authRepository.logout()
        // obrasRepository.clearLocalCache()
    }
}