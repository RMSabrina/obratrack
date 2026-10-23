package com.unaj.appmoviles.obratrack.core.data.repository

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FirebaseFirestoreException
import com.unaj.appmoviles.obratrack.core.domain.error.RelevamientoError
import kotlinx.coroutines.TimeoutCancellationException
import com.unaj.appmoviles.obratrack.core.domain.model.Relevamiento
import com.unaj.appmoviles.obratrack.core.domain.model.RelevamientoDraft
import com.unaj.appmoviles.obratrack.core.domain.model.TipoRelevamiento
import com.unaj.appmoviles.obratrack.core.domain.model.Ubicacion
import com.unaj.appmoviles.obratrack.core.firebase.ObraForbiddenException
import com.unaj.appmoviles.obratrack.core.firebase.ObraNotFoundException
import com.unaj.appmoviles.obratrack.core.firebase.RelevamientoDto
import com.unaj.appmoviles.obratrack.core.firebase.RelevamientoForbiddenException
import com.unaj.appmoviles.obratrack.core.firebase.RelevamientoNotFoundException

fun RelevamientoDto.toDomain(): Relevamiento = Relevamiento(
    id = id,
    obraId = obraId,
    userId = userId,
    fecha = fechaMillis,
    nota = nota,
    fotoUrl = fotoUrl,
    ubicacion = run {
        val lat = latitud
        val lng = longitud
        if (lat != null && lng != null) Ubicacion(lat, lng) else null
    },
    proximaVisita = proximaVisitaMillis,
    tipo = tipo?.ifBlank { null } ?: TipoRelevamiento.AVANCE,
    checklist = checklist.orEmpty()
)

fun RelevamientoDraft.toDto(userId: String, obraId: String): RelevamientoDto = RelevamientoDto(
    id = "",
    obraId = obraId,
    userId = userId,
    fechaMillis = fecha,
    nota = nota,
    fotoUrl = fotoUrl,
    latitud = ubicacion?.latitud,
    longitud = ubicacion?.longitud,
    proximaVisitaMillis = proximaVisita,
    tipo = tipo,
    checklist = checklist
)

fun Exception.toRelevamientoError(): RelevamientoError = when (this) {
    is TimeoutCancellationException -> RelevamientoError.Timeout
    is RelevamientoNotFoundException, is ObraNotFoundException -> RelevamientoError.NoEncontrado
    is RelevamientoForbiddenException, is ObraForbiddenException -> RelevamientoError.NoAutorizado
    is FirebaseNetworkException -> RelevamientoError.Red
    is FirebaseFirestoreException -> when (code) {
        FirebaseFirestoreException.Code.UNAVAILABLE,
        FirebaseFirestoreException.Code.DEADLINE_EXCEEDED -> RelevamientoError.Red
        FirebaseFirestoreException.Code.PERMISSION_DENIED -> RelevamientoError.NoAutorizado
        FirebaseFirestoreException.Code.NOT_FOUND -> RelevamientoError.NoEncontrado
        else -> RelevamientoError.Desconocido
    }
    else -> RelevamientoError.Desconocido
}
