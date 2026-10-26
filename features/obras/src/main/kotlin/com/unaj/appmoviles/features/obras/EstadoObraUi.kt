package com.unaj.appmoviles.features.obras

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.unaj.appmoviles.obras.R
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

@StringRes
fun EstadoObra.labelRes(): Int = when (this) {
    EstadoObra.ACTIVA -> R.string.estado_obra_activa
    EstadoObra.PAUSADA -> R.string.estado_obra_pausada
    EstadoObra.FINALIZADA -> R.string.estado_obra_finalizada
}

@Composable
fun EstadoObra.label(): String = stringResource(labelRes())
