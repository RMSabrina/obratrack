package com.unaj.appmoviles.obratrack.core.domain.usecase.location

import com.unaj.appmoviles.obratrack.core.domain.repository.GeocodingRepository
import javax.inject.Inject

class GetAddressFromUbicacionUseCase @Inject constructor(
    private val geocodingRepository: GeocodingRepository
) {
    suspend operator fun invoke(latitud: Double, longitud: Double): String? {
        return geocodingRepository.getAddressFromUbicacion(latitud, longitud)
    }
}
