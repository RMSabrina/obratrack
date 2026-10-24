package com.unaj.appmoviles.features.obras.detail

import androidx.annotation.StringRes
import com.unaj.appmoviles.obras.R
import com.unaj.appmoviles.obratrack.core.domain.model.Relevamiento
import com.unaj.appmoviles.obratrack.core.domain.model.ResumenTareas

/** Pestañas del detalle de obra, en el orden en que se muestran. */
enum class ObraDetailTab { RESUMEN, TAREAS, CRONOGRAMA, RELEVAMIENTOS }

@StringRes
internal fun ObraDetailTab.labelRes(): Int = when (this) {
    ObraDetailTab.RESUMEN -> R.string.obra_seccion_resumen
    ObraDetailTab.TAREAS -> R.string.obras_tareas
    ObraDetailTab.CRONOGRAMA -> R.string.cronograma_titulo
    ObraDetailTab.RELEVAMIENTOS -> R.string.obra_seccion_relevamientos
}

/** Acción del FAB del detalle según la pestaña. */
enum class FabDetalle { NUEVO_RELEVAMIENTO, NUEVA_TAREA }

/** Cantidad de relevamientos recientes que muestra la pestaña Resumen. */
const val RELEVAMIENTOS_EN_RESUMEN = 3

/** Tareas y cronograma son privados del dueño: los demás solo ven Resumen y Relevamientos. */
fun pestanasVisibles(esDueno: Boolean): List<ObraDetailTab> =
    if (esDueno) {
        ObraDetailTab.entries
    } else {
        listOf(ObraDetailTab.RESUMEN, ObraDetailTab.RELEVAMIENTOS)
    }

/** FAB de cada pestaña para el dueño; el cronograma no tiene. */
fun ObraDetailTab.fab(): FabDetalle? = when (this) {
    ObraDetailTab.RESUMEN, ObraDetailTab.RELEVAMIENTOS -> FabDetalle.NUEVO_RELEVAMIENTO
    ObraDetailTab.TAREAS -> FabDetalle.NUEVA_TAREA
    ObraDetailTab.CRONOGRAMA -> null
}

/** Número de la insignia de la pestaña Tareas; null si no hay atrasadas o no hay resumen. */
fun insigniaAtrasadas(resumen: ResumenTareas?): Int? =
    resumen?.atrasadas?.takeIf { it > 0 }

/** Los más recientes primero: la lista ya llega ordenada por fecha descendente. */
fun List<Relevamiento>.recientes(): List<Relevamiento> = take(RELEVAMIENTOS_EN_RESUMEN)
