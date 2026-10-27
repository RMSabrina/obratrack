package com.unaj.appmoviles.obratrack.core.domain.model

import java.time.LocalDate

/** Condiciones estimadas para la hora de una visita, en la zona horaria de la obra. */
data class PronosticoVisita(
    val fecha: LocalDate,
    val hora: Int,
    val temperaturaCelsius: Double,
    val probabilidadPrecipitacion: Int?,
    val vientoKmH: Double?,
    val codigoClima: Int
)
