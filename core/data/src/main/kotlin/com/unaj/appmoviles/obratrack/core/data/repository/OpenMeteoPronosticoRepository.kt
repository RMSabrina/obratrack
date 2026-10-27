package com.unaj.appmoviles.obratrack.core.data.repository

import com.unaj.appmoviles.obratrack.core.domain.model.PronosticoVisita
import com.unaj.appmoviles.obratrack.core.domain.repository.PronosticoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class OpenMeteoPronosticoRepository @Inject constructor() : PronosticoRepository {
    override suspend fun obtenerParaVisita(
        latitud: Double,
        longitud: Double,
        fecha: LocalDate,
        hora: Int?
    ): PronosticoVisita = withContext(Dispatchers.IO) {
        require(latitud.isFinite() && latitud in -90.0..90.0) { "Ubicación inválida" }
        require(longitud.isFinite() && longitud in -180.0..180.0) { "Ubicación inválida" }
        require(hora == null || hora in 0..1439) { "Hora inválida" }

        val hoy = LocalDate.now()
        require(!fecha.isBefore(hoy) && !fecha.isAfter(hoy.plusDays(MAX_DIAS_ADELANTE))) {
            "Fecha fuera del alcance del pronóstico"
        }
        val url = URL(
            "https://api.open-meteo.com/v1/forecast" +
                "?latitude=$latitud&longitude=$longitud" +
                "&hourly=temperature_2m,precipitation_probability,wind_speed_10m,weather_code" +
                "&timezone=auto&forecast_days=$DIAS_PRONOSTICO"
        )
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "ObraTrack-Android")
        }
        try {
            val status = connection.responseCode
            if (status !in 200..299) throw IllegalStateException("Pronóstico no disponible ($status)")
            val json = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            parseForecast(json, fecha, hora)
        } finally {
            connection.disconnect()
        }
    }

    private fun parseForecast(jsonText: String, fecha: LocalDate, horaVisita: Int?): PronosticoVisita {
        val root = JSONObject(jsonText)
        val hourly = root.getJSONObject("hourly")
        val times = hourly.getJSONArray("time")
        val temperatures = hourly.getJSONArray("temperature_2m")
        val probabilities = hourly.optJSONArray("precipitation_probability")
        val winds = hourly.optJSONArray("wind_speed_10m")
        val codes = hourly.getJSONArray("weather_code")
        val prefix = fecha.format(DateTimeFormatter.ISO_LOCAL_DATE)
        // Los índices de Open-Meteo son horas locales en timezone=auto. Para una visita sin hora
        // usamos las 12:00 locales, un punto representativo del día, en lugar de inventar 00:00.
        val desiredMinuteOfDay = horaVisita ?: 12 * 60
        var selected = -1
        var selectedHour = -1
        var bestDistance = Int.MAX_VALUE
        for (index in 0 until times.length()) {
            val stamp = times.optString(index)
            if (stamp.startsWith("$prefix")) {
                val hour = stamp.substringAfter('T').take(2).toIntOrNull() ?: continue
                val distance = kotlin.math.abs(hour * 60 - desiredMinuteOfDay)
                if (distance < bestDistance) {
                    selected = index
                    selectedHour = hour
                    bestDistance = distance
                }
            }
        }
        if (selected < 0 || selected >= temperatures.length() || selected >= codes.length()) {
            throw IllegalStateException("No hay datos horarios para la fecha de la visita")
        }
        val temperature = temperatures.optDouble(selected, Double.NaN)
        val code = codes.optInt(selected, -1)
        if (!temperature.isFinite() || code < 0) throw IllegalStateException("Pronóstico incompleto")
        val probability = probabilities?.takeIf { selected < it.length() }?.optInt(selected, -1)
            ?.takeIf { it in 0..100 }
        val wind = winds?.takeIf { selected < it.length() }?.optDouble(selected, Double.NaN)
            ?.takeIf { it.isFinite() && it >= 0 }

        return PronosticoVisita(
            fecha = fecha,
            hora = selectedHour * 60,
            temperaturaCelsius = temperature,
            probabilidadPrecipitacion = probability,
            vientoKmH = wind,
            codigoClima = code
        )
    }

    private companion object {
        const val TIMEOUT_MS = 8_000
        const val MAX_DIAS_ADELANTE = 15L
        const val DIAS_PRONOSTICO = 16
    }
}
