package com.unaj.appmoviles.features.obras

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.toFechaCalendario
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.toMillisCalendario
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Idioma elegido en la app (no el del sistema). Sin idioma elegido, o fuera de una Activity en
 * Android 12 o inferior (un Worker), AppCompat no lo conoce: ahí se pasa el [Locale] a mano.
 */
fun localeDeLaApp(): Locale =
    AppCompatDelegate.getApplicationLocales()[0] ?: Locale.getDefault()

/** Fecha de calendario (medianoche UTC en millis) en el formato del [locale]; el día no depende de la zona. */
fun formatFecha(millis: Long, locale: Locale = localeDeLaApp()): String =
    formatFecha(millis.toFechaCalendario(), locale)

fun formatFecha(fecha: LocalDate, locale: Locale = localeDeLaApp()): String =
    fecha.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))

/** Día y mes cortos para el encabezado del Gantt: "26 oct" en es e it, "Oct 26" en en. */
fun formatDiaMes(fecha: LocalDate, locale: Locale = localeDeLaApp()): String {
    val patron = if (locale.language == "en") "MMM d" else "d MMM"
    return fecha.format(DateTimeFormatter.ofPattern(patron, locale))
}

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

/** Fecha en el idioma de la app y hora con el formato 12/24 h del dispositivo. */
fun formatFechaHora(context: Context, momento: LocalDateTime): String =
    formatFecha(momento.toLocalDate()) + " " + formatHora(context, momento.hour * 60 + momento.minute)
