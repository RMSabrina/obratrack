package com.unaj.appmoviles.features.obras.relevamiento

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unaj.appmoviles.obratrack.core.domain.error.AppResult
import com.unaj.appmoviles.obratrack.core.domain.error.RelevamientoError
import com.unaj.appmoviles.obratrack.core.domain.model.RelevamientoDraft
import com.unaj.appmoviles.obratrack.core.domain.model.Tarea
import com.unaj.appmoviles.obratrack.core.domain.usecase.auth.ObserveSessionUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.ObserveObraUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.relevamientos.CreateRelevamientoUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.relevamientos.UpdateRelevamientoUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.tareas.ObserveTareasUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import com.unaj.appmoviles.features.obras.toMillisUtc
import java.time.Clock
import java.time.LocalDate
import dagger.hilt.android.lifecycle.HiltViewModel

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel(assistedFactory = RelevamientoFormViewModel.Factory::class)
class RelevamientoFormViewModel @AssistedInject constructor(
    private val observeSession: ObserveSessionUseCase,
    private val createRelevamiento: CreateRelevamientoUseCase,
    private val updateRelevamiento: UpdateRelevamientoUseCase,
    observeObra: ObserveObraUseCase,
    observeTareas: ObserveTareasUseCase,
    @Assisted("obraId") private val obraId: String,
    @Assisted("relevamientoId") private val relevamientoId: String?,
    /** Tarea vinculada al abrir el formulario (null al crear o si no tenía). */
    @Assisted("tareaId") tareaIdInicial: String?,
    private val clock: Clock
) : ViewModel() {

    @dagger.assisted.AssistedFactory
    interface Factory {
        fun create(
            @Assisted("obraId") obraId: String,
            @Assisted("relevamientoId") relevamientoId: String?,
            @Assisted("tareaId") tareaIdInicial: String?
        ): RelevamientoFormViewModel
    }

    /** Hoy (zona del dispositivo) como medianoche UTC, que es como se guardan las fechas de calendario. */
    val hoyMillisUtc: Long get() = LocalDate.now(clock).toMillisUtc()

    private val _uiState = MutableStateFlow<RelevamientoFormUiState>(RelevamientoFormUiState.Idle)
    val uiState: StateFlow<RelevamientoFormUiState> = _uiState.asStateFlow()

    private val _selector = MutableStateFlow(SelectorTareaUiState(seleccionada = tareaIdInicial))
    val selector: StateFlow<SelectorTareaUiState> = _selector.asStateFlow()

    private sealed interface LecturaTareas {
        data object NoDisponible : LecturaTareas
        data class Cargadas(val tareas: List<Tarea>) : LecturaTareas
        data object Error : LecturaTareas
    }

    init {
        viewModelScope.launch {
            observeSession().flatMapLatest { user ->
                if (user == null) return@flatMapLatest flowOf(LecturaTareas.NoDisponible)
                // Las tareas son privadas: solo se consultan si el usuario es dueño de la obra.
                observeObra(user.id, obraId)
                    .map { obra -> obra?.userId == user.id }
                    .distinctUntilChanged()
                    .flatMapLatest { esDueno ->
                        if (!esDueno) flowOf(LecturaTareas.NoDisponible) else leerTareas(observeTareas)
                    }
            }.collect { lectura ->
                _selector.update { actual ->
                    when (lectura) {
                        LecturaTareas.NoDisponible -> actual.copy(disponible = false, opciones = emptyList())
                        // Sin poder leer las tareas no se sabe si el vínculo sigue vigente: se conserva.
                        LecturaTareas.Error -> actual.copy(disponible = false, opciones = emptyList(), error = true)
                        is LecturaTareas.Cargadas -> SelectorTareaUiState(
                            disponible = true,
                            opciones = lectura.tareas.opcionesSelector(),
                            seleccionada = tareaVinculadaVigente(actual.seleccionada, lectura.tareas)
                        )
                    }
                }
            }
        }
    }

    private fun leerTareas(observeTareas: ObserveTareasUseCase): Flow<LecturaTareas> =
        observeTareas(obraId)
            .map<List<Tarea>, LecturaTareas> { LecturaTareas.Cargadas(it) }
            .catch { e ->
                Log.e(TAG, "No se pudieron leer las tareas", e)
                emit(LecturaTareas.Error)
            }

    /** null = "Ninguna". Solo acepta tareas de la obra. */
    fun onTareaSeleccionada(tareaId: String?) {
        _selector.update { actual ->
            if (!actual.disponible) return@update actual
            if (tareaId != null && actual.opciones.none { it.id == tareaId }) return@update actual
            actual.copy(seleccionada = tareaId)
        }
    }

    fun save(draft: RelevamientoDraft, photo: ByteArray?, continueAdding: Boolean = false) =
        guardar(draft, { photo }, continueAdding)

    /** [cargarFoto] lee la foto elegida (fuera del hilo principal); se llama una vez ya en "guardando". */
    fun guardar(draft: RelevamientoDraft, cargarFoto: suspend () -> ByteArray?, continueAdding: Boolean = false) {
        if (_uiState.value is RelevamientoFormUiState.Loading) return

        _uiState.value = RelevamientoFormUiState.Loading
        viewModelScope.launch {
            val photo = cargarFoto()
            // La foto se achica al subirla (core:data); este tope solo evita intentar con archivos enormes.
            if (photo != null && photo.size > MAX_FOTO_ORIGINAL_BYTES) {
                _uiState.value = RelevamientoFormUiState.Error(RelevamientoError.ImagenMuyGrande)
                return@launch
            }
            val user = observeSession().first()
            if (user == null) {
                _uiState.value = RelevamientoFormUiState.Error(RelevamientoError.NoAutorizado)
                return@launch
            }
            // El vínculo lo decide el selector, no la pantalla. Crear o editar un relevamiento
            // nunca cambia el estado de la tarea.
            val conTarea = draft.copy(tareaId = _selector.value.seleccionada)
            val result = if (relevamientoId == null) {
                createRelevamiento(user.id, obraId, conTarea, photo)
            } else {
                updateRelevamiento(user.id, obraId, relevamientoId, conTarea, photo)
            }
            _uiState.value = when (result) {
                is AppResult.Success -> {
                    if (continueAdding) {
                        RelevamientoFormUiState.SavedAndContinue("¡Evidencia guardada! Listo para la siguiente observación.")
                    } else {
                        RelevamientoFormUiState.Saved
                    }
                }
                is AppResult.Error -> RelevamientoFormUiState.Error(result.error)
            }
        }
    }

    fun onContinueReset() {
        if (_uiState.value is RelevamientoFormUiState.SavedAndContinue) {
            _uiState.value = RelevamientoFormUiState.Idle
        }
    }

    fun onErrorShown() {
        if (_uiState.value is RelevamientoFormUiState.Error) {
            _uiState.value = RelevamientoFormUiState.Idle
        }
    }

    internal companion object {
        const val MAX_FOTO_ORIGINAL_BYTES = 20 * 1024 * 1024
        private const val TAG = "RelevamientoFormVM"
    }
}
