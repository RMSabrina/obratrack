package com.unaj.appmoviles.obratrack.core.domain.model

enum class CategoriaAgendaVisita {
    URGENTE_VENCIDA,  // Programada para fechas pasadas y pendiente de relevamiento
    HOY,              // Programada para el día de la fecha
    ESTA_SEMANA,      // Programada para los próximos 7 días
    PROXIMAS,         // Programada para más de 7 días hacia adelante
    SIN_PROGRAMAR     // Obra activa que no tiene próxima visita fijada
}

data class ItemAgendaVisita(
    val obra: Obra,
    val fechaProgramadaMillis: Long?,
    val categoria: CategoriaAgendaVisita,
    val diasDiferencia: Long, // <0 vencida (ej. -2), 0 hoy, >0 días futuros
    val ultimaVisitaMillis: Long? = null,
    val diasSinVisita: Long? = null
)
