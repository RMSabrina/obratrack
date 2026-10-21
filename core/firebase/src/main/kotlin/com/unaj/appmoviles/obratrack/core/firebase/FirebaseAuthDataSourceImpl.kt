package com.unaj.appmoviles.obratrack.core.firebase

import android.util.Log
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.userProfileChangeRequest
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException
import javax.inject.Inject

class FirebaseAuthDataSourceImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) : FirebaseAuthDataSource {

    private val refreshTrigger = MutableStateFlow(0L)

    // Solo cuentan como "sesión" los usuarios con email verificado
    override val session: Flow<UserDto?> = combine(
        callbackFlow {
            val authStateListener = FirebaseAuth.AuthStateListener {
                trySend(Unit)
            }
            firebaseAuth.addAuthStateListener(authStateListener)
            awaitClose { firebaseAuth.removeAuthStateListener(authStateListener) }
        },
        refreshTrigger
    ) { _, _ ->
        val user = firebaseAuth.currentUser
        val isGoogleUser = user?.providerData?.any { it.providerId == GoogleAuthProvider.PROVIDER_ID } == true
        if (user != null && (user.isEmailVerified || isGoogleUser)) user.toDto() else null
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

    override suspend fun loginWithGoogle(idToken: String): FirebaseResult<UserDto> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val user = firebaseAuth.signInWithCredential(credential).await().user
                ?: return FirebaseResult.Error(Exception("Usuario nulo tras login con Google"))

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

    override suspend fun updateProfileName(newName: String): FirebaseResult<UserDto> =
        withContext(NonCancellable) {
            try {
                val user = firebaseAuth.currentUser
                    ?: return@withContext FirebaseResult.Error(Exception("Usuario no autenticado"))

                user.updateProfile(userProfileChangeRequest { displayName = newName }).await()
                user.reload().await()
                refreshTrigger.value++

                FirebaseResult.Success(user.toDto())
            } catch (e: Exception) {
                Log.e("VERIF", "Error al actualizar perfil", e)
                FirebaseResult.Error(e)
            }
        }

    override suspend fun updatePassword(newPassword: String): FirebaseResult<Unit> =
        withContext(NonCancellable) {
            try {
                val user = firebaseAuth.currentUser
                    ?: return@withContext FirebaseResult.Error(Exception("Usuario no autenticado"))

                user.updatePassword(newPassword).await()
                FirebaseResult.Success(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: FirebaseAuthRecentLoginRequiredException) {
                FirebaseResult.Error(e)
            } catch (e: Exception) {
                Log.e("AUTH", "Error al cambiar contraseña", e)
                FirebaseResult.Error(e)
            }
        }

    override suspend fun deleteAccount(): FirebaseResult<Unit> =
        withContext(NonCancellable) {
            try {
                val user = firebaseAuth.currentUser
                    ?: return@withContext FirebaseResult.Error(Exception("Usuario no autenticado"))

                user.delete().await()
                FirebaseResult.Success(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: FirebaseAuthRecentLoginRequiredException) {
                FirebaseResult.Error(e)
            } catch (e: Exception) {
                Log.e("AUTH", "Error al eliminar cuenta", e)
                FirebaseResult.Error(e)
            }
        }

    override suspend fun reauthenticate(password: String): FirebaseResult<Unit> =
        withContext(NonCancellable) {
            try {
                val user = firebaseAuth.currentUser
                    ?: return@withContext FirebaseResult.Error(Exception("Usuario no autenticado"))
                val email = user.email
                    ?: return@withContext FirebaseResult.Error(Exception("Email no disponible"))

                val credential = EmailAuthProvider.getCredential(email, password)
                user.reauthenticate(credential).await()
                FirebaseResult.Success(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("AUTH", "Error al reautenticar", e)
                FirebaseResult.Error(e)
            }
        }

    private fun FirebaseUser.toDto() = UserDto(
        uid = uid,
        email = email ?: "",
        displayName = displayName
    )
}