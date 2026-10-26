package com.unaj.appmoviles.notificaciones

import com.unaj.appmoviles.obratrack.core.domain.model.NotificacionItem

enum class NotificacionFiltro {
    TODAS,
    NO_LEIDAS,
    VISITAS,
    INCIDENTES
}

data class NotificacionesUiState(
    val items: List<NotificacionItem> = emptyList(),
    val filtro: NotificacionFiltro = NotificacionFiltro.TODAS,
    val isLoading: Boolean = false,
    val unreadCount: Int = 0
)
