package com.unaj.appmoviles.features.obras.tareas

import androidx.annotation.StringRes
import com.unaj.appmoviles.obras.R
import com.unaj.appmoviles.obratrack.core.domain.model.CategoriaTarea
import com.unaj.appmoviles.obratrack.core.domain.model.EstadoTarea
import com.unaj.appmoviles.obratrack.core.domain.model.Tarea
import com.unaj.appmoviles.obratrack.core.domain.model.TareaDraft

@StringRes
fun CategoriaTarea.labelRes(): Int = when (this) {
    CategoriaTarea.CIMIENTOS_Y_EXCAVACION -> R.string.tarea_categoria_cimientos_y_excavacion
    CategoriaTarea.ESTRUCTURA -> R.string.tarea_categoria_estructura
    CategoriaTarea.ALBANILERIA -> R.string.tarea_categoria_albanileria
    CategoriaTarea.TECHOS -> R.string.tarea_categoria_techos
    CategoriaTarea.ELECTRICIDAD -> R.string.tarea_categoria_electricidad
    CategoriaTarea.PLOMERIA -> R.string.tarea_categoria_plomeria
    CategoriaTarea.GAS -> R.string.tarea_categoria_gas
    CategoriaTarea.CLIMATIZACION -> R.string.tarea_categoria_climatizacion
    CategoriaTarea.CARPINTERIA -> R.string.tarea_categoria_carpinteria
    CategoriaTarea.HERRERIA -> R.string.tarea_categoria_herreria
    CategoriaTarea.PISOS_Y_REVESTIMIENTOS -> R.string.tarea_categoria_pisos_y_revestimientos
    CategoriaTarea.PINTURA -> R.string.tarea_categoria_pintura
    CategoriaTarea.ABERTURAS -> R.string.tarea_categoria_aberturas
    CategoriaTarea.IMPERMEABILIZACION -> R.string.tarea_categoria_impermeabilizacion
    CategoriaTarea.LIMPIEZA -> R.string.tarea_categoria_limpieza
    CategoriaTarea.OTROS -> R.string.tarea_categoria_otros
}

@StringRes
fun EstadoTarea.labelRes(): Int = when (this) {
    EstadoTarea.PENDIENTE -> R.string.tarea_estado_pendiente
    EstadoTarea.EN_CURSO -> R.string.tarea_estado_en_curso
    EstadoTarea.COMPLETADA -> R.string.tarea_estado_completada
}

/** Copia todos los campos editables de la tarea; base para editarla o completarla. */
internal fun Tarea.toDraft() = TareaDraft(
    titulo = titulo,
    descripcion = descripcion,
    categoria = categoria,
    fechaInicio = fechaInicio,
    fechaFin = fechaFin,
    estado = estado
)
