package com.unaj.appmoviles.obratrack.core.firebase

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import java.util.Date

/** Campo de la obra con la fecha de su relevamiento más reciente; lo escribe el alta de un relevamiento. */
internal const val OBRA_FIELD_ULTIMA_VISITA = "ultimaVisita"

private const val RELEVAMIENTO_FIELD_FECHA = "fecha"

/**
 * Fecha (millis, medianoche UTC del día) del relevamiento más reciente que queda, o null si no queda
 * ninguno. [fechas] es id -> fecha de todos los relevamientos de la obra; [quitar] son los que se
 * borran y [cambiar] el que cambia de fecha (id, fecha nueva).
 *
 * Se compara en millis: pasar millis a día de calendario (UTC) no invierte nunca el orden, así que el
 * máximo en millis es también el máximo por día, aunque algún dato viejo traiga hora.
 */
internal fun ultimaVisitaTras(
    fechas: Map<String, Long>,
    quitar: Set<String> = emptySet(),
    cambiar: Pair<String, Long>? = null
): Long? {
    val resultado = fechas.filterKeys { it !in quitar }.toMutableMap()
    if (cambiar != null) resultado[cambiar.first] = cambiar.second
    return resultado.values.maxOrNull()
}

/** Valor para escribir en la obra: la fecha, o el campo borrado si no queda ningún relevamiento. */
internal fun valorUltimaVisita(ultimaVisitaMillis: Long?): Any =
    ultimaVisitaMillis?.let { Timestamp(Date(it)) } ?: FieldValue.delete()

/** id -> fecha de los relevamientos leídos; ignora los que no tienen fecha. */
internal fun fechasPorId(docs: List<DocumentSnapshot>): Map<String, Long> =
    docs.mapNotNull { doc ->
        doc.getTimestamp(RELEVAMIENTO_FIELD_FECHA)?.toDate()?.time?.let { doc.id to it }
    }.toMap()
