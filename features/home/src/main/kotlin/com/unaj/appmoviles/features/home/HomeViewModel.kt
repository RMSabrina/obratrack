package com.unaj.appmoviles.features.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unaj.appmoviles.obratrack.core.domain.model.CategoriaAgendaVisita
import com.unaj.appmoviles.obratrack.core.domain.usecase.auth.ObserveSessionUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.ObserveAgendaVisitasUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.ObserveObrasUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.relevamientos.ObserveAllRelevamientosUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val observeSessionUseCase: ObserveSessionUseCase,
    private val observeObrasUseCase: ObserveObrasUseCase,
    private val observeAllRelevamientosUseCase: ObserveAllRelevamientosUseCase,
    private val observeAgendaVisitasUseCase: ObserveAgendaVisitasUseCase
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<HomeUiState> = observeSessionUseCase()
        .flatMapLatest { user ->
            if (user == null) {
                flowOf(HomeUiState(isLoading = false))
            } else {
                combine(
                    observeObrasUseCase(user.id),
                    observeAllRelevamientosUseCase(user.id).catch { e ->
                        Log.e("HomeViewModel", "Fallo al obtener relevamientos", e)
                        emit(emptyList())
                    },
                    observeAgendaVisitasUseCase(user.id).catch { e ->
                        Log.e("HomeViewModel", "Fallo al obtener agenda de visitas", e)
                        emit(emptyList())
                    }
                ) { obras, relevamientos, agendaItems ->
                    val recientes = relevamientos
                        .sortedByDescending { it.fecha }
                        .take(3)
                        .map { rel ->
                            val obra = obras.find { it.id == rel.obraId }
                            RelevamientoReciente(
                                relevamiento = rel,
                                obraNombre = obra?.nombre ?: "Obra desconocida"
                            )
                        }

                    val visitasHoy = agendaItems.count { it.categoria == CategoriaAgendaVisita.HOY }
                    val visitasAtrasadas = agendaItems.count { it.categoria == CategoriaAgendaVisita.URGENTE_VENCIDA }
                    val destacada = agendaItems.firstOrNull {
                        it.categoria == CategoriaAgendaVisita.HOY || it.categoria == CategoriaAgendaVisita.URGENTE_VENCIDA
                    } ?: agendaItems.firstOrNull { it.categoria == CategoriaAgendaVisita.ESTA_SEMANA }
                        
                    HomeUiState(
                        isLoading = false,
                        userName = user.name.ifBlank { "Usuario" },
                        obras = obras,
                        recientes = recientes,
                        proximaVisitaDestacada = destacada,
                        totalVisitasHoy = visitasHoy,
                        totalVisitasAtrasadas = visitasAtrasadas
                    )
                }.catch { e ->
                    Log.e("HomeViewModel", "Error general en Home", e)
                    emit(HomeUiState(isLoading = false, userName = user.name.ifBlank { "Usuario" }))
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState(isLoading = true)
        )
}