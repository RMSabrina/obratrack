package com.unaj.appmoviles.obratrack.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object LoginDestination : NavKey
@Serializable
data object RegisterDestination : NavKey

@Serializable
data object HomeDestination : NavKey

@Serializable
data object ObraSelectionDestination : NavKey

@Serializable
data object ObrasDestination : NavKey

@Serializable
data class ObraFormDestination(
    val obraId: String? = null,
    val nombre: String = "",
    val cliente: String = "",
    val direccion: String = "",
    val estado: String = "activa",
    val publica: Boolean = false
) : NavKey

@Serializable
data class ObraDetailDestination(
    val obraId: String
) : NavKey

@Serializable
data object ObrasPublicasDestination : NavKey

@Serializable
data class ObraPublicaDetailDestination(
    val obraId: String
) : NavKey

@Serializable
data class RelevamientoFormDestination(
    val obraId: String,
    val relevamientoId: String? = null,
    val nota: String = "",
    val fechaMillis: Long = 0L,
    val fotoUrl: String? = null,
    val latitud: Double? = null,
    val longitud: Double? = null
) : NavKey