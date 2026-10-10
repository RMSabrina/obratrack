package com.unaj.appmoviles.obratrack.core.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.userProfileChangeRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.cancellation.CancellationException

class FirebaseAuthDataSourceImpl(
    private val firebaseAuth: FirebaseAuth
) : FirebaseAuthDataSource {
    override val session: Flow<UserDto?> = callbackFlow {
        val authStateListener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser?.toDto())
        }
        firebaseAuth.addAuthStateListener(authStateListener)
        awaitClose {
            firebaseAuth.removeAuthStateListener(authStateListener)
        }
    }

    override suspend fun login(email: String, pass: String): FirebaseResult<UserDto> {
        return try {
            val user = firebaseAuth.signInWithEmailAndPassword(email, pass).await().user
                ?: return FirebaseResult.Error(Exception("Usuario nulo tras login"))
            FirebaseResult.Success(user.toDto())
        } catch (e: CancellationException) {
            throw e
        }
        catch (e: Exception) {
            FirebaseResult.Error(e)
        }
    }

    override suspend fun register(
        name: String,
        email: String,
        pass: String
    ): FirebaseResult<UserDto> {
        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, pass).await()
            val firebaseUser =
                result.user ?: return FirebaseResult.Error(Exception("Usuario nulo tras registro"))

            firebaseUser.updateProfile(
                userProfileChangeRequest { displayName = name }
            ).await()

            FirebaseResult.Success(firebaseUser.toDto())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FirebaseResult.Error(e)
        }
    }

    override suspend fun logout() {
        firebaseAuth.signOut()
    }

    private fun FirebaseUser.toDto() = UserDto(
        uid = uid,
        email = email ?: "",
        displayName = displayName
    )
}