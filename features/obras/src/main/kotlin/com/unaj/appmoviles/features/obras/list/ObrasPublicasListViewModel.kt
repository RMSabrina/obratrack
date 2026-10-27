package com.unaj.appmoviles.features.obras.list

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unaj.appmoviles.obratrack.core.domain.error.DomainError
import com.unaj.appmoviles.obratrack.core.domain.error.aErrorDeLectura
import com.unaj.appmoviles.obratrack.core.domain.model.Obra
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.ObserveObrasPublicasUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class ObrasPublicasListUiState(
    val obras: List<Obra> = emptyList(),
    val isLoading: Boolean = true,
    /** No se pudieron leer las obras: la pantalla muestra el error con "Reintentar", no "vacío". */
    val error: DomainError? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ObrasPublicasListViewModel @Inject constructor(
    observeObrasPublicasUseCase: ObserveObrasPublicasUseCase
) : ViewModel() {

    /** Cada incremento vuelve a leer las obras; solo lo hace el botón "Reintentar". */
    private val reintentos = MutableStateFlow(0)

    val uiState: StateFlow<ObrasPublicasListUiState> = reintentos
        .flatMapLatest {
            observeObrasPublicasUseCase()
                .map { obras -> ObrasPublicasListUiState(obras = obras, isLoading = false) }
                // Sin el catch, el error del Flow (por ejemplo, sin permisos) cerraría la app.
                .catch { e ->
                    Log.e(TAG, "No se pudieron leer las obras públicas", e)
                    emit(ObrasPublicasListUiState(isLoading = false, error = e.aErrorDeLectura()))
                }
                .onStart { emit(ObrasPublicasListUiState(isLoading = true)) }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ObrasPublicasListUiState(isLoading = true)
        )

    fun reintentar() {
        reintentos.update { it + 1 }
    }

    private companion object {
        const val TAG = "ObrasPublicasListVM"
    }
}
