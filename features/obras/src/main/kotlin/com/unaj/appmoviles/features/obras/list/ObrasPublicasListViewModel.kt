package com.unaj.appmoviles.features.obras.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unaj.appmoviles.obratrack.core.domain.error.AppResult
import com.unaj.appmoviles.obratrack.core.domain.model.Obra
import com.unaj.appmoviles.obratrack.core.domain.usecase.auth.ObserveSessionUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.ObserveSuscripcionesUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.ObserveObrasPublicasUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.SetSuscripcionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ObrasPublicasListUiState(
    val obras: List<Obra> = emptyList(),
    val suscripciones: Set<String> = emptySet(),
    val suscripcionesPendientes: Set<String> = emptySet(),
    val errorSuscripcion: Boolean = false,
    val isLoading: Boolean = true
)

@HiltViewModel
class ObrasPublicasListViewModel @Inject constructor(
    observeObrasPublicasUseCase: ObserveObrasPublicasUseCase,
    private val observeSession: ObserveSessionUseCase,
    observeSuscripciones: ObserveSuscripcionesUseCase,
    private val setSuscripcion: SetSuscripcionUseCase
) : ViewModel() {

    private val mutableUiState = MutableStateFlow(ObrasPublicasListUiState(isLoading = true))
    val uiState: StateFlow<ObrasPublicasListUiState> = mutableUiState

    init {
        viewModelScope.launch {
            combine(
                observeObrasPublicasUseCase(),
                observeSession().flatMapLatest { user ->
                    if (user == null) flowOf(emptySet()) else observeSuscripciones(user.id)
                }.catch { emit(emptySet()) }
            ) { obras, suscripciones ->
                mutableUiState.value.copy(
                    obras = obras,
                    suscripciones = suscripciones,
                    isLoading = false
                )
            }.collect { mutableUiState.value = it }
        }
    }

    fun toggleSuscripcion(obraId: String) {
        if (obraId in mutableUiState.value.suscripcionesPendientes) return
        viewModelScope.launch {
            val userId = observeSession().first()?.id ?: return@launch
            val seguir = obraId !in mutableUiState.value.suscripciones
            mutableUiState.update {
                it.copy(
                    suscripcionesPendientes = it.suscripcionesPendientes + obraId,
                    errorSuscripcion = false
                )
            }
            val result = setSuscripcion(userId, obraId, seguir)
            mutableUiState.update {
                it.copy(
                    suscripcionesPendientes = it.suscripcionesPendientes - obraId,
                    errorSuscripcion = result is AppResult.Error
                )
            }
        }
    }
}
