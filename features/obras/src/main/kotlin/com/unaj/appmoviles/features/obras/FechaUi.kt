package com.unaj.appmoviles.features.obras

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

fun formatFecha(millis: Long): String {
    val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("es-AR"))
    formatter.timeZone = TimeZone.getTimeZone("UTC")
    return formatter.format(Date(millis))
}
