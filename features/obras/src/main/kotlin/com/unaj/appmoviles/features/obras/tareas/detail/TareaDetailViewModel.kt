package com.unaj.appmoviles.features.obras.tareas.detail

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unaj.appmoviles.features.obras.tareas.toDraft
import com.unaj.appmoviles.obratrack.core.domain.error.AppResult
import com.unaj.appmoviles.obratrack.core.domain.error.DomainError
import com.unaj.appmoviles.obratrack.core.domain.error.TareaError
import com.unaj.appmoviles.obratrack.core.domain.error.aErrorDeLectura
import com.unaj.appmoviles.obratrack.core.domain.model.EstadoTarea
import com.unaj.appmoviles.obratrack.core.domain.model.Relevamiento
import com.unaj.appmoviles.obratrack.core.domain.model.RelevamientosVinculados
import com.unaj.appmoviles.obratrack.core.domain.model.Tarea
import com.unaj.appmoviles.obratrack.core.domain.usecase.auth.ObserveSessionUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.ObserveObraUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.relevamientos.ObserveRelevamientosDeTareaUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.tareas.DeleteTareaUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.tareas.ObserveTareaUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.tareas.UpdateTareaUseCase
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
@HiltViewModel(assistedFactory = TareaDetailViewModel.Factory::class)
class TareaDetailViewModel @AssistedInject constructor(
    @Assisted("obraId") private val obraId: String,
    @Assisted("tareaId") private val tareaId: String,
    observeSession: ObserveSessionUseCase,
    observeObra: ObserveObraUseCase,
    observeTarea: ObserveTareaUseCase,
    observeRelevamientosDeTarea: ObserveRelevamientosDeTareaUseCase,
    private val updateTarea: UpdateTareaUseCase,
    private val deleteTarea: DeleteTareaUseCase,
    private val clock: Clock
) : ViewModel() {

    @dagger.assisted.AssistedFactory
    interface Factory {
        fun create(
            @Assisted("obraId") obraId: String,
            @Assisted("tareaId") tareaId: String
        ): TareaDetailViewModel
    }

    private val _uiState = MutableStateFlow(TareaDetailUiState())
    val uiState: StateFlow<TareaDetailUiState> = _uiState.asStateFlow()

    /** Dueño de la obra; se guarda al verificarlo para usarlo al completar. */
    private var userId: String? = null

    /** Última lectura de la tarea, aunque la pantalla no se actualice durante el borrado. */
    private var ultimaTarea: Tarea? = null

    /** vinculados = null si no se pudieron leer. */
    private data class Lectura(
        val tarea: Tarea?,
        val error: DomainError?,
        val vinculados: List<Relevamiento>? = emptyList()
    )

    init {
        viewModelScope.launch {
            observeSession().flatMapLatest { user ->
                if (user == null) return@flatMapLatest flowOf(Lectura(null, TareaError.NoAutorizada))
                // Las tareas son privadas: solo se consultan si el usuario es dueño de la obra.
                observeObra(user.id, obraId)
                    .map { obra -> obra?.userId == user.id }
                    .distinctUntilChanged()
                    .flatMapLatest { esDueno ->
                        if (!esDueno) {
                            userId = null
                            flowOf(Lectura(null, TareaError.NoAutorizada))
                        } else {
                            userId = user.id
                            // Un error en los vinculados no oculta la tarea.
                            val vinculados = observeRelevamientosDeTarea(obraId, tareaId)
                                .map<List<Relevamiento>, List<Relevamiento>?> { it }
                                .catch { e ->
                                    Log.e(TAG, "No se pudieron leer los relevamientos vinculados", e)
                                    emit(null)
                                }
                            combine(observeTarea(obraId, tareaId), vinculados) { tarea, relevamientos ->
                                Lectura(tarea, if (tarea == null) TareaError.NoEncontrada else null, relevamientos)
                            }
                        }
                    }
            }.catch { e ->
                Log.e(TAG, "No se pudo leer la tarea", e)
                emit(Lectura(null, e.aErrorDeLectura(TareaError.Desconocido)))
            }.collect { lectura ->
                ultimaTarea = lectura.tarea
                // Durante el borrado se mantiene la tarea en pantalla hasta saber el resultado.
                if (_uiState.value.isDeleting) return@collect
                val tarea = lectura.tarea
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        tarea = tarea,
                        atrasada = tarea?.estaAtrasada(LocalDate.now(clock)) ?: false,
                        error = lectura.error,
                        vinculados = lectura.vinculados.orEmpty(),
                        vinculadosError = lectura.vinculados == null
                    )
                }
            }
        }
    }

    /** Solo cambia el estado a COMPLETADA; el resto de los campos se conserva. */
    fun completar() {
        val state = _uiState.value
        val tarea = state.tarea ?: return
        val owner = userId ?: return
        if (state.isCompleting || state.isDeleting || tarea.estado == EstadoTarea.COMPLETADA) return
        _uiState.update { it.copy(isCompleting = true, actionError = null) }
        viewModelScope.launch {
            val draft = tarea.toDraft().copy(estado = EstadoTarea.COMPLETADA)
            val result = updateTarea(owner, obraId, tareaId, draft)
            _uiState.update {
                when (result) {
                    // La tarea actualizada llega por el Flow de observeTarea.
                    is AppResult.Success -> it.copy(isCompleting = false)
                    is AppResult.Error -> it.copy(isCompleting = false, actionError = result.error)
                }
            }
        }
    }

    /**
     * [vinculados]: qué hacer con los relevamientos vinculados (lo elige el usuario en el diálogo).
     * Sin poder leer los vinculados no se borra: no se sabría qué se está eliminando.
     */
    fun eliminar(vinculados: RelevamientosVinculados = RelevamientosVinculados.DESVINCULAR) {
        val state = _uiState.value
        if (state.isDeleting || state.tarea == null || state.vinculadosError) return
        _uiState.update { it.copy(isDeleting = true, deleteError = null) }
        viewModelScope.launch {
            when (val result = deleteTarea(obraId, tareaId, vinculados)) {
                is AppResult.Success -> _uiState.update { it.copy(isDeleted = true) }
                is AppResult.Error -> {
                    // Sin conexión, Firestore aplica el borrado en el caché local y la
                    // confirmación tarda: si la tarea ya no está, se da por borrada.
                    if (result.error == TareaError.Timeout && ultimaTarea == null) {
                        _uiState.update { it.copy(isDeleted = true) }
                    } else {
                        _uiState.update { it.copy(isDeleting = false, deleteError = result.error) }
                    }
                }
            }
        }
    }

    fun onActionErrorShown() {
        _uiState.update { it.copy(actionError = null) }
    }

    fun onDeleteErrorShown() {
        _uiState.update { it.copy(deleteError = null) }
    }

    private companion object {
        const val TAG = "TareaDetailViewModel"
    }
}
