package com.unaj.appmoviles.obratrack.core.data.repository

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FirebaseFirestoreException
import com.unaj.appmoviles.obratrack.core.domain.error.ObraError
import com.unaj.appmoviles.obratrack.core.domain.model.EstadoObra
import com.unaj.appmoviles.obratrack.core.domain.model.Obra
import com.unaj.appmoviles.obratrack.core.domain.model.ObraDraft
import com.unaj.appmoviles.obratrack.core.firebase.ObraDto
import com.unaj.appmoviles.obratrack.core.firebase.ObraForbiddenException
import com.unaj.appmoviles.obratrack.core.firebase.ObraNotFoundException

fun ObraDto.toDomain(): Obra = Obra(
    id = id,
    userId = userId,
    nombre = nombre,
    cliente = cliente,
    direccion = direccion,
    publica = publica,
    estado = estado.toEstadoObra(),
    fechaInicio = fechaInicioMillis,
    ultimaVisita = ultimaVisitaMillis
)

fun ObraDraft.toDto(userId: String, fechaInicioMillis: Long): ObraDto = ObraDto(
    id = "",
    userId = userId,
    nombre = nombre,
    cliente = cliente,
    publica = publica,
    direccion = direccion,
    fechaInicioMillis = fechaInicioMillis,
    estado = estado.toFirestoreValue(),
    ultimaVisitaMillis = null,
    suscriptores = emptyList()
)

fun Exception.toObraError(): ObraError = when (this) {
    is ObraNotFoundException -> ObraError.NoEncontrada
    is ObraForbiddenException -> ObraError.NoAutorizada
    is FirebaseNetworkException -> ObraError.Red
    is FirebaseFirestoreException -> when (code) {
        FirebaseFirestoreException.Code.UNAVAILABLE,
        FirebaseFirestoreException.Code.DEADLINE_EXCEEDED -> ObraError.Red
        FirebaseFirestoreException.Code.PERMISSION_DENIED -> ObraError.NoAutorizada
        FirebaseFirestoreException.Code.NOT_FOUND -> ObraError.NoEncontrada
        else -> ObraError.Desconocido
    }
    else -> ObraError.Desconocido
}

private fun EstadoObra.toFirestoreValue(): String = when (this) {
    EstadoObra.ACTIVA -> "activa"
    EstadoObra.PAUSADA -> "pausada"
    EstadoObra.FINALIZADA -> "finalizada"
}

private fun String.toEstadoObra(): EstadoObra = when (this) {
    "pausada" -> EstadoObra.PAUSADA
    "finalizada" -> EstadoObra.FINALIZADA
    else -> EstadoObra.ACTIVA
}
