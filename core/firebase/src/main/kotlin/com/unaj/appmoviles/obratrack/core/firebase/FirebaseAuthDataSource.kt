package com.unaj.appmoviles.obratrack.core.firebase

import kotlinx.coroutines.flow.Flow

interface FirebaseAuthDataSource {
    val session: Flow<UserDto?>
    suspend fun login(email: String, pass: String): FirebaseResult<UserDto>
    suspend fun register(name: String, email: String, pass: String): FirebaseResult<UserDto>

    suspend fun resendVerificationEmail(email: String, pass: String): FirebaseResult<Unit>
    suspend fun logout()
}