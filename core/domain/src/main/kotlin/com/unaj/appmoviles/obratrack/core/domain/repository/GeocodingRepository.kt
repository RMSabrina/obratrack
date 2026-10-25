package com.unaj.appmoviles.obratrack.core.domain.repository

import com.unaj.appmoviles.obratrack.core.domain.model.Ubicacion

interface GeocodingRepository {
    suspend fun getUbicacionFromAddress(direccion: String): Ubicacion?
    suspend fun getAddressFromUbicacion(latitud: Double, longitud: Double): String?
}
