package com.unaj.appmoviles.obratrack.core.firebase

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.userProfileChangeRequest
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

class FirebaseAuthDataSourceImpl(
    private val firebaseAuth: FirebaseAuth
) : FirebaseAuthDataSource {

    // Solo cuentan como "sesión" los usuarios con email verificado
    override val session: Flow<UserDto?> = callbackFlow {
        val authStateListener = FirebaseAuth.AuthStateListener { auth ->
            val user = auth.currentUser
            trySend(if (user != null && user.isEmailVerified) user.toDto() else null)
        }
        firebaseAuth.addAuthStateListener(authStateListener)
        awaitClose { firebaseAuth.removeAuthStateListener(authStateListener) }
    }

    override suspend fun login(email: String, pass: String): FirebaseResult<UserDto> {
        return try {
            val user = firebaseAuth.signInWithEmailAndPassword(email, pass).await().user
                ?: return FirebaseResult.Error(Exception("Usuario nulo tras login"))

            if (!user.isEmailVerified) {
                firebaseAuth.signOut()
                return FirebaseResult.Error(Exception("EMAIL_NOT_VERIFIED"))
            }
            FirebaseResult.Success(user.toDto())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FirebaseResult.Error(e)
        }
    }

    override suspend fun register(
        name: String,
        email: String,
        pass: String
    ): FirebaseResult<UserDto> = withContext(NonCancellable) {
        try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, pass).await()
            val firebaseUser = result.user
                ?: return@withContext FirebaseResult.Error(Exception("Usuario nulo tras registro"))

            firebaseUser.updateProfile(userProfileChangeRequest { displayName = name }).await()
            firebaseUser.sendEmailVerification().await()
            Log.d("VERIF", "Correo de verificación enviado a ${firebaseUser.email}")

            val dto = firebaseUser.toDto()
            firebaseAuth.signOut()
            FirebaseResult.Success(dto)
        } catch (e: Exception) {
            Log.e("VERIF", "Error en registro/verificación", e)
            FirebaseResult.Error(e)
        }
    }

    override suspend fun logout() {
        firebaseAuth.signOut()
    }

    override suspend fun resendVerificationEmail(email: String, pass: String): FirebaseResult<Unit> =
        withContext(NonCancellable) {
            try {
                val user = firebaseAuth.signInWithEmailAndPassword(email, pass).await().user
                    ?: return@withContext FirebaseResult.Error(Exception("Usuario nulo"))
                if (user.isEmailVerified) {
                    firebaseAuth.signOut()
                    return@withContext FirebaseResult.Error(Exception("EMAIL_ALREADY_VERIFIED"))
                }
                user.sendEmailVerification().await()
                firebaseAuth.signOut()
                FirebaseResult.Success(Unit)
            } catch (e: Exception) {
                FirebaseResult.Error(e)
            }
        }

    private fun FirebaseUser.toDto() = UserDto(
        uid = uid,
        email = email ?: "",
        displayName = displayName
    )
}