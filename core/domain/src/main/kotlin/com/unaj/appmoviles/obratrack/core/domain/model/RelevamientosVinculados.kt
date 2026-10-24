package com.unaj.appmoviles.obratrack.core.domain.model

/**
 * Qué pasa con los relevamientos vinculados al borrar una tarea. Lo elige el usuario;
 * nunca se borran relevamientos sin su confirmación.
 */
enum class RelevamientosVinculados {
    /** Se conservan, sin tarea vinculada (opción por defecto). */
    DESVINCULAR,
    /** Se eliminan junto con la tarea. */
    ELIMINAR
}
