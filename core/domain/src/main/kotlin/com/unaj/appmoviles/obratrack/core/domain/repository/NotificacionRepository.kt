package com.unaj.appmoviles.obratrack.core.domain.repository

import com.unaj.appmoviles.obratrack.core.domain.model.NotificacionItem
import kotlinx.coroutines.flow.Flow

interface NotificacionRepository {
    fun observe(userId: String): Flow<List<NotificacionItem>>
    suspend fun marcarComoLeida(id: String): Result<Unit>
    suspend fun marcarTodasComoLeidas(userId: String): Result<Unit>
    suspend fun crearNotificacion(notificacion: NotificacionItem): Result<Unit>
    suspend fun eliminarNotificacion(id: String): Result<Unit>
}
