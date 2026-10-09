package com.unaj.appmoviles.obratrack.core.domain.usecase

import com.unaj.appmoviles.obratrack.core.domain.repository.AuthRepository

class LogoutUseCase(
    private val authRepository: AuthRepository
    // Para el futuro: private val obrasRepository: ObrasRepository
) {
    suspend operator fun invoke() {
        authRepository.logout()
        // obrasRepository.clearLocalCache()
    }
}