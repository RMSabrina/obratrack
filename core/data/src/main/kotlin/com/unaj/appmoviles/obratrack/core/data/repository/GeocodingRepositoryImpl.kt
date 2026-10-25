package com.unaj.appmoviles.obratrack.core.data.repository

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import com.unaj.appmoviles.obratrack.core.domain.model.Ubicacion
import com.unaj.appmoviles.obratrack.core.domain.repository.GeocodingRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class GeocodingRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : GeocodingRepository {

    override suspend fun getUbicacionFromAddress(direccion: String): Ubicacion? = withContext(Dispatchers.IO) {
        try {
            if (!Geocoder.isPresent()) return@withContext null
            val geocoder = Geocoder(context, Locale.getDefault())

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { continuation ->
                    try {
                        geocoder.getFromLocationName(direccion, 1) { addresses ->
                            val first = addresses.firstOrNull()
                            val ubicacion = first?.let { Ubicacion(it.latitude, it.longitude) }
                            if (continuation.isActive) {
                                continuation.resume(ubicacion)
                            }
                        }
                    } catch (e: Exception) {
                        if (continuation.isActive) {
                            continuation.resume(null)
                        }
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocationName(direccion, 1)
                val first = addresses?.firstOrNull()
                first?.let { Ubicacion(it.latitude, it.longitude) }
            }
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun getAddressFromUbicacion(latitud: Double, longitud: Double): String? = withContext(Dispatchers.IO) {
        try {
            if (!Geocoder.isPresent()) return@withContext null
            val geocoder = Geocoder(context, Locale.getDefault())

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { continuation ->
                    try {
                        geocoder.getFromLocation(latitud, longitud, 1) { addresses ->
                            val first = addresses.firstOrNull()
                            val formatted = formatAddress(first)
                            if (continuation.isActive) {
                                continuation.resume(formatted)
                            }
                        }
                    } catch (e: Exception) {
                        if (continuation.isActive) {
                            continuation.resume(null)
                        }
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(latitud, longitud, 1)
                val first = addresses?.firstOrNull()
                formatAddress(first)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun formatAddress(address: Address?): String? {
        if (address == null) return null
        val line = address.getAddressLine(0)
        return if (!line.isNullOrBlank()) {
            line
        } else {
            val parts = listOfNotNull(
                address.thoroughfare?.let { street ->
                    val number = address.subThoroughfare
                    if (!number.isNullOrBlank()) "$street $number" else street
                },
                address.locality,
                address.adminArea
            ).filter { it.isNotBlank() }
            if (parts.isNotEmpty()) parts.joinToString(", ") else null
        }
    }
}
