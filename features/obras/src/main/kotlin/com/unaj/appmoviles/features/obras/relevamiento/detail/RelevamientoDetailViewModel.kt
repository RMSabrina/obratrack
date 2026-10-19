package com.unaj.appmoviles.features.obras.relevamiento.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unaj.appmoviles.obratrack.core.domain.usecase.auth.ObserveSessionUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.ObserveObraPublicaUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.ObserveObraUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.relevamientos.ObserveRelevamientosDeObraUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel

@HiltViewModel(assistedFactory = RelevamientoDetailViewModel.Factory::class)
class RelevamientoDetailViewModel @AssistedInject constructor(
    @Assisted("obraId") private val obraId: String,
    @Assisted("relevamientoId") private val relevamientoId: String,
    observeSession: ObserveSessionUseCase,
    private val observeObra: ObserveObraUseCase,
    private val observeObraPublica: ObserveObraPublicaUseCase,
    private val observeRelevamientosDeObra: ObserveRelevamientosDeObraUseCase
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("obraId") obraId: String,
            @Assisted("relevamientoId") relevamientoId: String
        ): RelevamientoDetailViewModel
    }

    private val _uiState = MutableStateFlow(RelevamientoDetailUiState())
    val uiState: StateFlow<RelevamientoDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeSession().collectLatest { user ->
                val obraFlow = if (user != null) {
                    observeObra(user.id, obraId).flatMapLatest { obraPropia ->
                        if (obraPropia != null) flowOf(obraPropia)
                        else observeObraPublica(obraId)
                    }
                } else {
                    observeObraPublica(obraId)
                }

                combine(
                    obraFlow,
                    observeRelevamientosDeObra(obraId)
                ) { obra, relevamientos ->
                    val relevamiento = relevamientos.find { it.id == relevamientoId }
                    RelevamientoDetailUiState(
                        isLoading = false,
                        isEditable = obra != null && user != null && obra.userId == user.id,
                        obra = obra,
                        relevamiento = relevamiento,
                        error = if (relevamiento == null) "No encontramos este relevamiento" else null
                    )
                }.catch {
                    _uiState.value = RelevamientoDetailUiState(
                        isLoading = false,
                        error = "Ocurrió un error al cargar el relevamiento"
                    )
                }.collect {
                    _uiState.value = it
                }
            }
        }
    }
}
