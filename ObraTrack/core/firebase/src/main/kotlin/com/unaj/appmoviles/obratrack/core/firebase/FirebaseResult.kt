package com.unaj.appmoviles.obratrack.core.firebase

sealed interface FirebaseResult<out T> {
    data class Success<T>(val data: T) : FirebaseResult<T>
    data class Error(val exception: Exception) : FirebaseResult<Nothing>
}