package com.unaj.appmoviles.obratrack.core.firebase
/**
 * Modelo de Firebase, no tiene lógica de negocio.
 */
data class UserDto(
    val uid: String,
    val email: String,
    val displayName: String?
)