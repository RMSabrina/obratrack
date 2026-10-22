package com.unaj.appmoviles.obratrack.core.data.repository

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FirebaseFirestoreException
import com.unaj.appmoviles.obratrack.core.domain.error.TareaError
import com.unaj.appmoviles.obratrack.core.domain.model.CategoriaTarea
import com.unaj.appmoviles.obratrack.core.domain.model.EstadoTarea
import com.unaj.appmoviles.obratrack.core.domain.model.Tarea
import com.unaj.appmoviles.obratrack.core.domain.model.TareaDraft
import com.unaj.appmoviles.obratrack.core.firebase.ObraForbiddenException
import com.unaj.appmoviles.obratrack.core.firebase.ObraNotFoundException
import com.unaj.appmoviles.obratrack.core.firebase.TareaDto
import com.unaj.appmoviles.obratrack.core.firebase.TareaForbiddenException
import com.unaj.appmoviles.obratrack.core.firebase.TareaNotFoundException
import kotlinx.coroutines.TimeoutCancellationException
import java.time.LocalDate
import java.time.format.DateTimeParseException

fun TareaDto.toDomain(): Tarea {
    val (inicio, fin) = fechasValidas(fechaInicio, fechaFin)
    return Tarea(
        id = id,
        obraId = obraId,
        userId = userId,
        titulo = titulo,
        descripcion = descripcion,
        categoria = CategoriaTarea.entries.firstOrNull { it.name == categoria } ?: CategoriaTarea.OTROS,
        fechaInicio = inicio,
        fechaFin = fin,
        estado = EstadoTarea.entries.firstOrNull { it.name == estado } ?: EstadoTarea.PENDIENTE
    )
}

fun TareaDraft.toDto(userId: String, obraId: String): TareaDto = TareaDto(
    id = "",
    obraId = obraId,
    userId = userId,
    titulo = titulo,
    descripcion = descripcion,
    categoria = categoria.name,
    estado = estado.name,
    fechaInicio = fechaInicio?.toString(),
    fechaFin = fechaFin?.toString()
)

fun Exception.toTareaError(): TareaError = when (this) {
    is TimeoutCancellationException -> TareaError.Timeout
    is TareaNotFoundException, is ObraNotFoundException -> TareaError.NoEncontrada
    is TareaForbiddenException, is ObraForbiddenException -> TareaError.NoAutorizada
    is FirebaseNetworkException -> TareaError.Red
    is FirebaseFirestoreException -> when (code) {
        FirebaseFirestoreException.Code.UNAVAILABLE,
        FirebaseFirestoreException.Code.DEADLINE_EXCEEDED -> TareaError.Red
        FirebaseFirestoreException.Code.PERMISSION_DENIED -> TareaError.NoAutorizada
        FirebaseFirestoreException.Code.NOT_FOUND -> TareaError.NoEncontrada
        else -> TareaError.Desconocido
    }
    else -> TareaError.Desconocido
}

/**
 * Las fechas van en par: si falta una, alguna no es ISO válida o fin < inicio,
 * la tarea se lee como "sin fechas".
 */
private fun fechasValidas(inicio: String?, fin: String?): Pair<LocalDate?, LocalDate?> {
    val desde = inicio?.toLocalDateOrNull()
    val hasta = fin?.toLocalDateOrNull()
    return if (desde != null && hasta != null && !hasta.isBefore(desde)) desde to hasta else null to null
}

private fun String.toLocalDateOrNull(): LocalDate? = try {
    LocalDate.parse(this)
} catch (e: DateTimeParseException) {
    null
}
