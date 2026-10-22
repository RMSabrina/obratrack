package com.unaj.appmoviles.features.obras

import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import java.util.TimeZone

fun formatFecha(millis: Long): String {
    val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("es-AR"))
    formatter.timeZone = TimeZone.getTimeZone("UTC")
    return formatter.format(Date(millis))
}

private val fechaFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.forLanguageTag("es-AR"))

fun formatFecha(fecha: LocalDate): String = fecha.format(fechaFormatter)

/** El DatePicker de Material 3 devuelve la medianoche UTC del día elegido: se lee en UTC. */
fun millisUtcToLocalDate(millis: Long): LocalDate =
    Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()

/** Inversa de [millisUtcToLocalDate], para preseleccionar una fecha en el DatePicker. */
fun LocalDate.toMillisUtc(): Long =
    atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
