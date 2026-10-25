package com.unaj.appmoviles.features.obras.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unaj.appmoviles.obratrack.core.domain.error.AppResult
import com.unaj.appmoviles.obratrack.core.domain.error.ObraError
import com.unaj.appmoviles.obratrack.core.domain.model.Relevamiento
import com.unaj.appmoviles.obratrack.core.domain.model.Tarea
import com.unaj.appmoviles.obratrack.core.domain.usecase.auth.ObserveSessionUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.DeleteObraUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.ObserveObraUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.relevamientos.ObserveRelevamientosUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.tareas.ObserveTareasUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.tareas.resumen
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate

import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel(assistedFactory = ObraDetailViewModel.Factory::class)
class ObraDetailViewModel @AssistedInject constructor(
    @Assisted private val obraId: String,
    observeSession: ObserveSessionUseCase,
    private val observeObra: ObserveObraUseCase,
    private val observeRelevamientos: ObserveRelevamientosUseCase,
    private val observeTareas: ObserveTareasUseCase,
    private val deleteObraUseCase: DeleteObraUseCase,
    private val clock: Clock
) : ViewModel() {

    @dagger.assisted.AssistedFactory
    interface Factory {
        fun create(obraId: String): ObraDetailViewModel
    }

    private val _uiState = MutableStateFlow(ObraDetailUiState())
    val uiState: StateFlow<ObraDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeSession().collectLatest { user ->
                if (user == null) {
                    _uiState.update { it.copy(isLoading = false, error = ObraError.NoAutorizada) }
                    return@collectLatest
                }
                combine(
                    observeObra(user.id, obraId),
                    observeRelevamientosOError(user.id),
                    observeTareasOError(user.id)
                ) { obra, relevamientos, tareas ->
                    val esDueno = obra?.userId == user.id
                    // tareas == null: no se pudieron leer; el resumen se oculta.
                    val resumenTareas = if (esDueno) tareas?.resumen(LocalDate.now(clock)) else null
                    val current = _uiState.value
                    if (current.isDeleting) {
                        if (obra == null && current.deleteError == ObraError.Timeout) {
                            current.copy(isDeleted = true)
                        } else {
                            current
                        }
                    } else {
                        ObraDetailUiState(
                            isLoading = false,
                            isEditable = esDueno,
                            obra = obra,
                            relevamientos = relevamientos.orEmpty(),
                            relevamientosError = relevamientos == null,
                            tareas = if (esDueno) tareas.orEmpty() else emptyList(),
                            resumenTareas = resumenTareas,
                            error = if (obra == null) ObraError.NoEncontrada else null,
                            isDeleting = current.isDeleting,
                            isDeleted = current.isDeleted,
                            deleteError = current.deleteError
                        )
                    }
                }.catch {
                    if (!_uiState.value.isDeleting) {
                        _uiState.update { it.copy(isLoading = false, error = ObraError.Desconocido) }
                    }
                }.collect { newState ->
                    _uiState.value = newState
                }
            }
        }
    }

    /**
     * null si fallan los relevamientos: la obra se sigue mostrando y la sección avisa del error
     * (si no, el error cortaría el combine y la lista quedaría vacía como si no hubiera ninguno).
     */
    private fun observeRelevamientosOError(userId: String): Flow<List<Relevamiento>?> =
        observeRelevamientos(userId, obraId)
            .map<List<Relevamiento>, List<Relevamiento>?> { it }
            .catch { emit(null) }

    /**
     * Las tareas son privadas: solo se observan si el usuario es dueño (si no, Firestore
     * rechaza la lectura). Un error de tareas devuelve null (se oculta el resumen) sin
     * afectar al resto.
     */
    private fun observeTareasOError(userId: String): Flow<List<Tarea>?> =
        observeObra(userId, obraId)
            .map { obra -> obra?.userId == userId }
            .distinctUntilChanged()
            .flatMapLatest { esDueno ->
                if (!esDueno) {
                    flowOf(emptyList())
                } else {
                    observeTareas(obraId)
                        .map<List<Tarea>, List<Tarea>?> { it }
                        .catch { emit(null) }
                }
            }

    fun deleteObra() {
        if (_uiState.value.isDeleting) return
        _uiState.update { it.copy(isDeleting = true, deleteError = null) }
        viewModelScope.launch {
            when (val result = deleteObraUseCase(obraId)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(isDeleting = true, isDeleted = true) }
                }
                is AppResult.Error -> {
                    if (result.error == ObraError.Timeout) {
                        if (_uiState.value.obra == null) {
                            _uiState.update { it.copy(isDeleting = true, isDeleted = true) }
                        } else {
                            _uiState.update { it.copy(isDeleting = false, deleteError = ObraError.Timeout) }
                        }
                    } else {
                        _uiState.update { it.copy(isDeleting = false, deleteError = result.error) }
                    }
                }
            }
        }
    }

    fun onDeleteErrorDismissed() {
        _uiState.update { it.copy(deleteError = null) }
    }
}
