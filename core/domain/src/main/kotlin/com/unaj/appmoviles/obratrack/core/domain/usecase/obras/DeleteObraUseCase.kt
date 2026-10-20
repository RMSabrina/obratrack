package com.unaj.appmoviles.obratrack.core.domain.usecase.obras

import com.unaj.appmoviles.obratrack.core.domain.error.AppResult
import com.unaj.appmoviles.obratrack.core.domain.error.ObraError
import com.unaj.appmoviles.obratrack.core.domain.error.RelevamientoError
import com.unaj.appmoviles.obratrack.core.domain.repository.ObraRepository
import com.unaj.appmoviles.obratrack.core.domain.repository.RelevamientoRepository
import javax.inject.Inject

class DeleteObraUseCase @Inject constructor(
    private val obraRepository: ObraRepository,
    private val relevamientoRepository: RelevamientoRepository
) {
    suspend operator fun invoke(obraId: String): AppResult<Unit> {
        // 1. Verificación inicial de existencia y propiedad
        when (val existsRes = obraRepository.existsAndOwnedByCurrentUser(obraId)) {
            is AppResult.Success -> {
                if (!existsRes.data) return AppResult.Success(Unit)
            }
            is AppResult.Error -> return AppResult.Error(existsRes.error)
        }

        // 2. Cascade paso 1: Borrar relevamientos
        when (val res1 = relevamientoRepository.deleteAllByObra(obraId)) {
            is AppResult.Success -> Unit
            is AppResult.Error -> {
                if (res1.error == RelevamientoError.NoAutorizado) {
                    val doubleCheck = obraRepository.existsAndOwnedByCurrentUser(obraId)
                    if (doubleCheck is AppResult.Success && !doubleCheck.data) {
                        return AppResult.Success(Unit)
                    }
                }
                return AppResult.Error(ObraError.FalloParcial)
            }
        }

        // 3. Cascade paso 2: Borrar suscriptores
        when (val res2 = obraRepository.deleteAllSuscriptoresByObra(obraId)) {
            is AppResult.Success -> Unit
            is AppResult.Error -> {
                if (res2.error == ObraError.NoAutorizada) {
                    val doubleCheck = obraRepository.existsAndOwnedByCurrentUser(obraId)
                    if (doubleCheck is AppResult.Success && !doubleCheck.data) {
                        return AppResult.Success(Unit)
                    }
                }
                return AppResult.Error(ObraError.FalloParcial)
            }
        }

        // 4. Cascade paso 3: Borrar Obra
        return when (val res3 = obraRepository.delete(obraId)) {
            is AppResult.Success -> AppResult.Success(Unit)
            is AppResult.Error -> {
                if (res3.error == ObraError.NoAutorizada) {
                    val doubleCheck = obraRepository.existsAndOwnedByCurrentUser(obraId)
                    if (doubleCheck is AppResult.Success && !doubleCheck.data) {
                        return AppResult.Success(Unit)
                    }
                }
                AppResult.Error(res3.error)
            }
        }
    }
}
