package com.unaj.appmoviles.obratrack.core.firebase

import com.google.firebase.auth.FirebaseUser

/**
 * Modelo de Firebase, no tiene lógica de negocio.
 */
data class UserDto(
    val uid: String,
    val email: String,
    val displayName: String?
)

