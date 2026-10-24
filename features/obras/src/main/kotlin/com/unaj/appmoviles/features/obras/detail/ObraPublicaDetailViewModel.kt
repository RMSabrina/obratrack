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
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class ObraPublicaDetailUiState(
    val obra: Obra? = null,
    val relevamientos: List<Relevamiento> = emptyList(),
    /** No se pudieron leer los relevamientos: la sección muestra un mensaje, no "vacío". */
    val relevamientosError: Boolean = false,
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
        // null si fallan: sin el catch, el error del Flow cerraría la app en vez de avisar.
        observeRelevamientosUseCase(obraId)
            .map<List<Relevamiento>, List<Relevamiento>?> { it }
            .catch { emit(null) },
        internalError
    ) { obra, relevamientos, error ->
        ObraPublicaDetailUiState(
            obra = obra,
            relevamientos = relevamientos.orEmpty(),
            relevamientosError = relevamientos == null,
            isLoading = false,
            error = error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ObraPublicaDetailUiState(isLoading = true)
    )
}
