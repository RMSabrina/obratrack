package com.unaj.appmoviles.features.obras.tareas.cronograma

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unaj.appmoviles.obratrack.core.domain.error.TareaError
import com.unaj.appmoviles.obratrack.core.domain.usecase.auth.ObserveSessionUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.ObserveObraUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.tareas.ObserveTareasUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel(assistedFactory = CronogramaViewModel.Factory::class)
class CronogramaViewModel @AssistedInject constructor(
    @Assisted private val obraId: String,
    observeSession: ObserveSessionUseCase,
    observeObra: ObserveObraUseCase,
    observeTareas: ObserveTareasUseCase,
    private val clock: Clock
) : ViewModel() {

    @dagger.assisted.AssistedFactory
    interface Factory {
        fun create(obraId: String): CronogramaViewModel
    }

    private val _uiState = MutableStateFlow(CronogramaUiState())
    val uiState: StateFlow<CronogramaUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeSession().flatMapLatest { user ->
                if (user == null) return@flatMapLatest flowOf(noAutorizado())
                // El cronograma es privado: las tareas solo se consultan si el usuario es dueño.
                observeObra(user.id, obraId)
                    .map { obra -> obra?.userId == user.id }
                    .distinctUntilChanged()
                    .flatMapLatest { esDueno ->
                        if (!esDueno) {
                            flowOf(noAutorizado())
                        } else {
                            observeTareas(obraId).map { tareas ->
                                CronogramaUiState(
                                    isLoading = false,
                                    cronograma = calcularCronograma(tareas, LocalDate.now(clock))
                                )
                            }
                        }
                    }
            }.catch {
                emit(CronogramaUiState(isLoading = false, error = TareaError.Desconocido))
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    private fun noAutorizado() = CronogramaUiState(isLoading = false, error = TareaError.NoAutorizada)
}
