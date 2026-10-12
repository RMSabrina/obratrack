package com.unaj.appmoviles.features.obras

import com.unaj.appmoviles.obratrack.core.domain.model.EstadoObra

fun EstadoObra.toStorageValue(): String = when (this) {
    EstadoObra.ACTIVA -> "activa"
    EstadoObra.PAUSADA -> "pausada"
    EstadoObra.FINALIZADA -> "finalizada"
}

fun estadoObraFromStorage(value: String): EstadoObra = when (value) {
    "pausada" -> EstadoObra.PAUSADA
    "finalizada" -> EstadoObra.FINALIZADA
    else -> EstadoObra.ACTIVA
}

fun EstadoObra.label(): String = when (this) {
    EstadoObra.ACTIVA -> "Activa"
    EstadoObra.PAUSADA -> "Pausada"
    EstadoObra.FINALIZADA -> "Finalizada"
}
