package com.unaj.appmoviles.features.obras

import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.toFechaCalendario
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.toMillisCalendario
import android.content.Context
import java.text.SimpleDateFormat
import java.time.LocalDate
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
fun millisUtcToLocalDate(millis: Long): LocalDate = millis.toFechaCalendario()

/** Inversa de [millisUtcToLocalDate], para preseleccionar una fecha en el DatePicker. */
fun LocalDate.toMillisUtc(): Long = toMillisCalendario()

/** Hora de una visita (minutos desde la medianoche) con el formato 12/24 h del dispositivo. */
fun formatHora(context: Context, minutos: Int): String {
    val formato = android.text.format.DateFormat.getTimeFormat(context)
    formato.timeZone = TimeZone.getTimeZone("UTC")
    return formato.format(Date(minutos * 60_000L))
}
