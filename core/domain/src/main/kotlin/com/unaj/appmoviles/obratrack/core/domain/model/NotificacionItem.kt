package com.unaj.appmoviles.obratrack.core.domain.model

enum class TipoNotificacion {
    VISITA_RECORDATORIO,
    VISITA_ATRASADA,
    OBRA_INACTIVA,
    INCIDENTE_REPORTADO,
    OBRA_ASIGNADA,
    NOVEDAD_PUBLICADA,
    SINCRONIZACION_EXITOSA
}

data class NotificacionItem(
    val id: String,
    val userId: String,
    val obraId: String? = null,
    val titulo: String,
    val mensaje: String,
    val tipo: TipoNotificacion,
    val fecha: Long,
    val leida: Boolean = false,
    val rutaDestino: String? = null
)
