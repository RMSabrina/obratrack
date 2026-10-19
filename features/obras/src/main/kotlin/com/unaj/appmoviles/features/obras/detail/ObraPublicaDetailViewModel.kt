package com.unaj.appmoviles.features.obras.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unaj.appmoviles.obratrack.core.domain.model.Obra
import com.unaj.appmoviles.obratrack.core.domain.model.Relevamiento
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.ObserveObraPublicaUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.relevamientos.ObserveRelevamientosDeObraUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class ObraPublicaDetailUiState(
    val obra: Obra? = null,
    val relevamientos: List<Relevamiento> = emptyList(),
    val isLoading: Boolean = true,
    val error: Throwable? = null
)

@HiltViewModel(assistedFactory = ObraPublicaDetailViewModel.Factory::class)
class ObraPublicaDetailViewModel @AssistedInject constructor(
    @Assisted obraId: String,
    observeObraPublicaUseCase: ObserveObraPublicaUseCase,
    observeRelevamientosUseCase: ObserveRelevamientosDeObraUseCase
) : ViewModel() {

    @dagger.assisted.AssistedFactory
    interface Factory {
        fun create(obraId: String): ObraPublicaDetailViewModel
    }

    private val internalError = MutableStateFlow<Throwable?>(null)

    val uiState: StateFlow<ObraPublicaDetailUiState> = combine(
        observeObraPublicaUseCase(obraId),
        observeRelevamientosUseCase(obraId),
        internalError
    ) { obra, relevamientos, error ->
        ObraPublicaDetailUiState(
            obra = obra,
            relevamientos = relevamientos,
            isLoading = false,
            error = error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ObraPublicaDetailUiState(isLoading = true)
    )
}
