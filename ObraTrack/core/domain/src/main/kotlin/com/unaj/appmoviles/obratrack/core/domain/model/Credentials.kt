package com.unaj.appmoviles.obratrack.core.domain.model

data class Credentials(
    val email: String,
    val password: String
){
    override fun toString() = "Credentials(email=$email, password=***)"
}