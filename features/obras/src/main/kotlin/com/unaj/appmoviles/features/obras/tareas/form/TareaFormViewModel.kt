package com.unaj.appmoviles.features.obras.tareas.form

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unaj.appmoviles.features.obras.tareas.toDraft
import com.unaj.appmoviles.obratrack.core.domain.error.AppResult
import com.unaj.appmoviles.obratrack.core.domain.error.TareaError
import com.unaj.appmoviles.obratrack.core.domain.model.TareaDraft
import com.unaj.appmoviles.obratrack.core.domain.usecase.auth.ObserveSessionUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.ObserveObraUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.tareas.CreateTareaUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.tareas.ObserveTareaUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.tareas.UpdateTareaUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = TareaFormViewModel.Factory::class)
class TareaFormViewModel @AssistedInject constructor(
    @Assisted("obraId") private val obraId: String,
    @Assisted("tareaId") private val tareaId: String?,
    private val observeSession: ObserveSessionUseCase,
    private val observeObra: ObserveObraUseCase,
    private val observeTarea: ObserveTareaUseCase,
    private val createTarea: CreateTareaUseCase,
    private val updateTarea: UpdateTareaUseCase
) : ViewModel() {

    @dagger.assisted.AssistedFactory
    interface Factory {
        fun create(
            @Assisted("obraId") obraId: String,
            @Assisted("tareaId") tareaId: String?
        ): TareaFormViewModel
    }

    private val _uiState = MutableStateFlow(TareaFormUiState())
    val uiState: StateFlow<TareaFormUiState> = _uiState.asStateFlow()

    /** Dueño de la obra; se guarda al cargar para usarlo al crear o editar. */
    private var userId: String? = null

    init {
        viewModelScope.launch {
            _uiState.value = cargar()
        }
    }

    private suspend fun cargar(): TareaFormUiState {
        val user = observeSession().first() ?: return fallo(TareaError.NoAutorizada)
        // Las tareas son privadas: solo el dueño de la obra puede abrir el formulario.
        val obra = observeObra(user.id, obraId)
            .catch { e ->
                Log.e(TAG, "No se pudo leer la obra", e)
                emit(null)
            }
            .first()
        if (obra?.userId != user.id) return fallo(TareaError.NoAutorizada)
        userId = user.id

        if (tareaId == null) {
            return TareaFormUiState(isLoading = false, inicial = TareaDraft(titulo = ""))
        }
        val tarea = observeTarea(obraId, tareaId)
            .catch { e ->
                Log.e(TAG, "No se pudo leer la tarea", e)
                emit(null)
            }
            .first()
            ?: return fallo(TareaError.NoEncontrada)
        return TareaFormUiState(isLoading = false, inicial = tarea.toDraft())
    }

    fun save(draft: TareaDraft) {
        val current = _uiState.value
        val owner = userId
        if (current.isSaving || current.inicial == null || owner == null) return
        _uiState.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            val result = if (tareaId == null) {
                createTarea(owner, obraId, draft)
            } else {
                updateTarea(owner, obraId, tareaId, draft)
            }
            _uiState.update {
                when (result) {
                    is AppResult.Success -> it.copy(isSaving = false, isSaved = true)
                    is AppResult.Error -> it.copy(isSaving = false, error = result.error)
                }
            }
        }
    }

    /** Al cambiar un campo se oculta el error anterior. */
    fun onInputChanged() {
        if (_uiState.value.error != null && _uiState.value.inicial != null) {
            _uiState.update { it.copy(error = null) }
        }
    }

    private fun fallo(error: TareaError) = TareaFormUiState(isLoading = false, error = error)

    private companion object {
        const val TAG = "TareaFormViewModel"
    }
}
