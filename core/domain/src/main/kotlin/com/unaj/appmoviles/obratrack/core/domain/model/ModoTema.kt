package com.unaj.appmoviles.obratrack.core.domain.model

/** Tema elegido en la app. Se guarda por `name`: agregar un valor es seguro, renombrar uno rompe lo guardado. */
enum class ModoTema {
    SISTEMA,
    CLARO,
    OSCURO;

    companion object {
        /** Valor ausente o desconocido: sigue al sistema. */
        fun desdeNombre(nombre: String?): ModoTema = entries.firstOrNull { it.name == nombre } ?: SISTEMA
    }
}
