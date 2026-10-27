package com.unaj.appmoviles.features.home.agenda

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unaj.appmoviles.obratrack.core.domain.error.aErrorDeLectura
import com.unaj.appmoviles.obratrack.core.domain.model.Obra
import com.unaj.appmoviles.obratrack.core.domain.usecase.auth.ObserveSessionUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.ObserveAgendaVisitasUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.ScheduleVisitaUseCase
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
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AgendaViewModel @Inject constructor(
    private val observeSessionUseCase: ObserveSessionUseCase,
    private val observeAgendaVisitasUseCase: ObserveAgendaVisitasUseCase,
    private val scheduleVisitaUseCase: ScheduleVisitaUseCase
) : ViewModel() {

    private val filtroFlow = MutableStateFlow(FiltroAgenda.TODAS)
    private val obraParaReprogramarFlow = MutableStateFlow<Obra?>(null)
    private val isUpdatingFlow = MutableStateFlow(false)
    private val feedbackMessageFlow = MutableStateFlow<FeedbackAgenda?>(null)

    /** Cada incremento vuelve a leer la agenda; solo lo hace el botón "Reintentar". */
    private val reintentos = MutableStateFlow(0)

    val uiState: StateFlow<AgendaUiState> = combine(observeSessionUseCase(), reintentos) { user, _ -> user }
        .flatMapLatest { user ->
            if (user == null) {
                flowOf(AgendaUiState(isLoading = false))
            } else {
                combine(
                    observeAgendaVisitasUseCase(user.id),
                    filtroFlow,
                    obraParaReprogramarFlow,
                    isUpdatingFlow,
                    feedbackMessageFlow
                ) { items, filtro, obraParaReprogramar, isUpdating, feedback ->
                    AgendaUiState(
                        isLoading = false,
                        items = items,
                        filtro = filtro,
                        obraParaReprogramar = obraParaReprogramar,
                        isUpdating = isUpdating,
                        mensajeFeedback = feedback
                    )
                }.catch { e ->
                    Log.e(TAG, "No se pudo cargar la agenda", e)
                    emit(AgendaUiState(isLoading = false, error = e.aErrorDeLectura()))
                }.onStart { emit(AgendaUiState(isLoading = true)) }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AgendaUiState(isLoading = true)
        )

    fun reintentar() {
        reintentos.update { it + 1 }
    }

    fun onFiltroChanged(nuevoFiltro: FiltroAgenda) {
        filtroFlow.value = nuevoFiltro
    }

    fun onIniciarReprogramacion(obra: Obra) {
        obraParaReprogramarFlow.value = obra
    }

    fun onCancelarReprogramacion() {
        obraParaReprogramarFlow.value = null
    }

    fun onConfirmarReprogramacion(obra: Obra, nuevoMillis: Long?) {
        viewModelScope.launch {
            isUpdatingFlow.value = true
            obraParaReprogramarFlow.value = null
            val res = scheduleVisitaUseCase(obra.userId, obra, nuevoMillis)
            feedbackMessageFlow.value = feedbackDeReprogramacion(obra.nombre, nuevoMillis, res)
            isUpdatingFlow.value = false
        }
    }

    fun onClearFeedbackMessage() {
        feedbackMessageFlow.value = null
    }

    private companion object {
        const val TAG = "AgendaViewModel"
    }
}
