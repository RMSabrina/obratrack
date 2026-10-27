package com.unaj.appmoviles.features.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unaj.appmoviles.obratrack.core.domain.error.aErrorDeLectura
import com.unaj.appmoviles.obratrack.core.domain.model.CategoriaAgendaVisita
import com.unaj.appmoviles.obratrack.core.domain.usecase.auth.ObserveSessionUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.ObserveAgendaVisitasUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.ObserveObrasUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.relevamientos.ObserveAllRelevamientosUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val observeSessionUseCase: ObserveSessionUseCase,
    private val observeObrasUseCase: ObserveObrasUseCase,
    private val observeAllRelevamientosUseCase: ObserveAllRelevamientosUseCase,
    private val observeAgendaVisitasUseCase: ObserveAgendaVisitasUseCase
) : ViewModel() {

    /** Cada incremento vuelve a leer todo; solo lo hace el botón "Reintentar". */
    private val reintentos = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<HomeUiState> = combine(observeSessionUseCase(), reintentos) { user, _ -> user }
        .flatMapLatest { user ->
            if (user == null) {
                flowOf(HomeUiState(isLoading = false))
            } else {
                combine(
                    observeObrasUseCase(user.id),
                    observeAllRelevamientosUseCase(user.id),
                    observeAgendaVisitasUseCase(user.id)
                ) { obras, relevamientos, agendaItems ->
                    val recientes = relevamientosRecientes(relevamientos, obras)

                    val visitasHoy = agendaItems.count { it.categoria == CategoriaAgendaVisita.HOY }
                    val visitasAtrasadas = agendaItems.count { it.categoria == CategoriaAgendaVisita.URGENTE_VENCIDA }
                    val destacada = agendaItems.firstOrNull {
                        it.categoria == CategoriaAgendaVisita.HOY || it.categoria == CategoriaAgendaVisita.URGENTE_VENCIDA
                    } ?: agendaItems.firstOrNull { it.categoria == CategoriaAgendaVisita.ESTA_SEMANA }
                        
                    HomeUiState(
                        isLoading = false,
                        userName = user.name,
                        obras = obras,
                        recientes = recientes,
                        proximaVisitaDestacada = destacada,
                        totalVisitasHoy = visitasHoy,
                        totalVisitasAtrasadas = visitasAtrasadas
                    )
                }.catch { e ->
                    // Una lectura que falla no se muestra como "sin datos": la pantalla ofrece reintentar.
                    Log.e("HomeViewModel", "No se pudo cargar el Home", e)
                    emit(HomeUiState(isLoading = false, userName = user.name, error = e.aErrorDeLectura()))
                }.onStart { emit(HomeUiState(isLoading = true)) }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState(isLoading = true)
        )

    fun reintentar() {
        reintentos.update { it + 1 }
    }
}