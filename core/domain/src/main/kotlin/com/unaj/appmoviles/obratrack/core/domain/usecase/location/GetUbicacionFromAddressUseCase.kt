package com.unaj.appmoviles.obratrack.core.domain.usecase.location

import com.unaj.appmoviles.obratrack.core.domain.model.Ubicacion
import com.unaj.appmoviles.obratrack.core.domain.repository.GeocodingRepository
import javax.inject.Inject

class GetUbicacionFromAddressUseCase @Inject constructor(
    private val geocodingRepository: GeocodingRepository
) {
    suspend operator fun invoke(direccion: String): Ubicacion? {
        val clean = direccion.trim()
        if (clean.isBlank()) return null
        return geocodingRepository.getUbicacionFromAddress(clean)
    }
}
