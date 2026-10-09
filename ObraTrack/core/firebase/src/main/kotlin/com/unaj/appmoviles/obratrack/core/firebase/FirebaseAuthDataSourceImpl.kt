package com.unaj.appmoviles.obratrack.core.firebase

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseAuthDataSourceImpl (
    private val firebaseAuth: FirebaseAuth
) : FirebaseAuthDataSource {
    override val session: Flow<UserDto?> = callbackFlow {
        val authStateListener = FirebaseAuth.AuthStateListener { auth ->
            val user = auth.currentUser
            val userDto = user?.let {
                UserDto(
                    uid = it.uid,
                    email = it.email ?: "",
                    displayName = it.displayName
                )
            }
            trySend(userDto)
        }

        firebaseAuth.addAuthStateListener(authStateListener)

        awaitClose {
            firebaseAuth.removeAuthStateListener(authStateListener)
        }
    }

    override suspend fun login(email: String, pass: String): FirebaseResult<UserDto> {
        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, pass).await()
            val firebaseUser = result.user

            if (firebaseUser != null) {
                val userDto = UserDto(
                    uid = firebaseUser.uid,
                    email = firebaseUser.email ?: "",
                    displayName = firebaseUser.displayName
                )
                FirebaseResult.Success(userDto)
            } else {
                FirebaseResult.Error(Exception("Usuario nulo tras login"))
            }
        } catch (e: Exception) {
            FirebaseResult.Error(e)
        }
    }

    override suspend fun register(name: String, email: String, pass: String): FirebaseResult<UserDto> {
        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, pass).await()
            val firebaseUser = result.user

            if(firebaseUser != null){
                val userDto = UserDto(
                    uid = firebaseUser.uid,
                    email = firebaseUser.email ?: "",
                    displayName = name
                )
                FirebaseResult.Success(userDto)
            } else{
                FirebaseResult.Error(Exception("Usuario nulo tras registro"))
            }
        } catch (e: Exception) {
            FirebaseResult.Error(e)
        }
    }

    override suspend fun logout() {
        firebaseAuth.signOut()
    }
}