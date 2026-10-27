package com.unaj.appmoviles.features.obras.tareas.list

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unaj.appmoviles.obratrack.core.domain.error.TareaError
import com.unaj.appmoviles.obratrack.core.domain.error.aErrorDeLectura
import com.unaj.appmoviles.obratrack.core.domain.usecase.auth.ObserveSessionUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.ObserveObraUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.tareas.ObserveTareasUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.tareas.estaAtrasada
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel(assistedFactory = TareasListViewModel.Factory::class)
class TareasListViewModel @AssistedInject constructor(
    @Assisted private val obraId: String,
    observeSession: ObserveSessionUseCase,
    observeObra: ObserveObraUseCase,
    observeTareas: ObserveTareasUseCase,
    private val clock: Clock
) : ViewModel() {

    @dagger.assisted.AssistedFactory
    interface Factory {
        fun create(obraId: String): TareasListViewModel
    }

    private val _uiState = MutableStateFlow(TareasListUiState())
    val uiState: StateFlow<TareasListUiState> = _uiState.asStateFlow()

    /** Cada incremento vuelve a leer las tareas; solo lo hace el botón "Reintentar". */
    private val reintentos = MutableStateFlow(0)

    init {
        viewModelScope.launch {
            combine(observeSession(), reintentos) { user, _ -> user }.flatMapLatest { user ->
                if (user == null) return@flatMapLatest flowOf(noAutorizado())
                // Las tareas son privadas: solo se consultan si el usuario es dueño de la obra.
                observeObra(user.id, obraId)
                    .map { obra -> obra?.userId == user.id }
                    .distinctUntilChanged()
                    .flatMapLatest { esDueno ->
                        if (!esDueno) {
                            flowOf(noAutorizado())
                        } else {
                            observeTareas(obraId).map { tareas ->
                                val hoy = LocalDate.now(clock)
                                TareasListUiState(
                                    isLoading = false,
                                    tareas = tareas.map { TareaItem(it, it.estaAtrasada(hoy)) }
                                )
                            }
                        }
                    }
                    // El catch va acá adentro: si cortara todo el flujo, "Reintentar" ya no tendría efecto.
                    .catch { e ->
                        Log.e(TAG, "No se pudieron leer las tareas", e)
                        emit(TareasListUiState(isLoading = false, error = e.aErrorDeLectura(TareaError.Desconocido)))
                    }
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    fun reintentar() {
        _uiState.value = TareasListUiState()
        reintentos.update { it + 1 }
    }

    private companion object {
        const val TAG = "TareasListViewModel"
    }

    private fun noAutorizado() = TareasListUiState(isLoading = false, error = TareaError.NoAutorizada)
}
