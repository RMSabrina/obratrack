package com.unaj.appmoviles.obratrack.core.domain.model

data class Colaborador(
    val id: String,
    val obraId: String,
    val userId: String,
    val email: String,
    val nombre: String,
    val rol: UserRole,
    val fechaAsignacion: Long
)
