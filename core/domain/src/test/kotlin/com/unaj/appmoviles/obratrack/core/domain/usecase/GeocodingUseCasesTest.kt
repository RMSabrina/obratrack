package com.unaj.appmoviles.obratrack.core.domain.usecase

import com.unaj.appmoviles.obratrack.core.domain.model.Ubicacion
import com.unaj.appmoviles.obratrack.core.domain.repository.GeocodingRepository
import com.unaj.appmoviles.obratrack.core.domain.usecase.location.GetAddressFromUbicacionUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.location.GetUbicacionFromAddressUseCase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GeocodingUseCasesTest {

    private val fakeRepository = object : GeocodingRepository {
        val addressMap = mutableMapOf(
            "Av. San Martín 1500" to Ubicacion(-34.7963, -58.2764)
        )
        val locationMap = mutableMapOf(
            Pair(-34.7963, -58.2764) to "Av. San Martín 1500, Florencio Varela"
        )

        override suspend fun getUbicacionFromAddress(direccion: String): Ubicacion? {
            return addressMap[direccion]
        }

        override suspend fun getAddressFromUbicacion(latitud: Double, longitud: Double): String? {
            return locationMap[Pair(latitud, longitud)]
        }
    }

    private val getUbicacion = GetUbicacionFromAddressUseCase(fakeRepository)
    private val getAddress = GetAddressFromUbicacionUseCase(fakeRepository)

    @Test
    fun `buscar direccion existente retorna ubicacion`() = runBlocking {
        val result = getUbicacion("Av. San Martín 1500")
        assertEquals(Ubicacion(-34.7963, -58.2764), result)
    }

    @Test
    fun `buscar direccion en blanco retorna null`() = runBlocking {
        val result = getUbicacion("   ")
        assertNull(result)
    }

    @Test
    fun `geocodificacion inversa retorna direccion formateada`() = runBlocking {
        val result = getAddress(-34.7963, -58.2764)
        assertEquals("Av. San Martín 1500, Florencio Varela", result)
    }

    @Test
    fun `geocodificacion inversa de punto desconocido retorna null`() = runBlocking {
        val result = getAddress(0.0, 0.0)
        assertNull(result)
    }
}
