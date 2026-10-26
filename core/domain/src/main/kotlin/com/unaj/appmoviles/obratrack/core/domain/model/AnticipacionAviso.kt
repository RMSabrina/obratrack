package com.unaj.appmoviles.obratrack.core.domain.model

/** Con cuánta anticipación se avisa de una visita. Se guarda por `name`: agregar es seguro, renombrar no. */
enum class AnticipacionAviso(val minutos: Long) {
    AL_MOMENTO(0),
    MIN_15(15),
    HORA_1(60),
    DIA_1(24 * 60);

    companion object {
        val POR_DEFECTO = HORA_1
    }
}
