package com.unaj.appmoviles.notificaciones

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unaj.appmoviles.obratrack.core.domain.model.TipoNotificacion
import com.unaj.appmoviles.obratrack.core.domain.repository.AuthRepository
import com.unaj.appmoviles.obratrack.core.domain.repository.NotificacionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class NotificacionesViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val notificacionRepository: NotificacionRepository
) : ViewModel() {

    private val filtroState = MutableStateFlow(NotificacionFiltro.TODAS)

    val uiState: StateFlow<NotificacionesUiState> = authRepository.session
        .flatMapLatest { user ->
            if (user == null) {
                flowOf(emptyList())
            } else {
                notificacionRepository.observe(user.id)
            }
        }
        .combine(filtroState) { allItems, filtro ->
            val unreadCount = allItems.count { !it.leida }
            val filtered = when (filtro) {
                NotificacionFiltro.TODAS -> allItems
                NotificacionFiltro.NO_LEIDAS -> allItems.filter { !it.leida }
                NotificacionFiltro.VISITAS -> allItems.filter {
                    it.tipo == TipoNotificacion.VISITA_RECORDATORIO ||
                            it.tipo == TipoNotificacion.VISITA_ATRASADA ||
                            it.tipo == TipoNotificacion.OBRA_INACTIVA
                }
                NotificacionFiltro.INCIDENTES -> allItems.filter {
                    it.tipo == TipoNotificacion.INCIDENTE_REPORTADO
                }
            }
            NotificacionesUiState(
                items = filtered,
                filtro = filtro,
                isLoading = false,
                unreadCount = unreadCount
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = NotificacionesUiState(isLoading = true)
        )

    fun onFilterSelected(filtro: NotificacionFiltro) {
        filtroState.value = filtro
    }

    fun onMarcarComoLeida(id: String) {
        viewModelScope.launch {
            notificacionRepository.marcarComoLeida(id)
        }
    }

    fun onMarcarTodasComoLeidas() {
        viewModelScope.launch {
            val user = authRepository.session.firstOrNull()
            if (user != null) {
                notificacionRepository.marcarTodasComoLeidas(user.id)
            }
        }
    }
}
