package com.unaj.appmoviles.obratrack.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object LoginDestination : NavKey
@Serializable
data object RegisterDestination : NavKey

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