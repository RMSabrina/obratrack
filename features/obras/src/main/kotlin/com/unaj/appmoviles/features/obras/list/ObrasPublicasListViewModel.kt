package com.unaj.appmoviles.features.obras.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unaj.appmoviles.obratrack.core.domain.model.Obra
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.ObserveObrasPublicasUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class ObrasPublicasListUiState(
    val obras: List<Obra> = emptyList(),
    val isLoading: Boolean = true
)

class ObrasPublicasListViewModel(
    observeObrasPublicasUseCase: ObserveObrasPublicasUseCase
) : ViewModel() {

    val uiState: StateFlow<ObrasPublicasListUiState> = observeObrasPublicasUseCase()
        .map { obras ->
            ObrasPublicasListUiState(
                obras = obras,
                isLoading = false
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ObrasPublicasListUiState(isLoading = true)
        )
}
