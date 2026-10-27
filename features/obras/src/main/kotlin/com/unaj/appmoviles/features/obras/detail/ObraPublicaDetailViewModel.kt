package com.unaj.appmoviles.features.obras.detail

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unaj.appmoviles.obratrack.core.domain.error.DomainError
import com.unaj.appmoviles.obratrack.core.domain.error.RelevamientoError
import com.unaj.appmoviles.obratrack.core.domain.error.aErrorDeLectura
import com.unaj.appmoviles.obratrack.core.domain.model.Obra
import com.unaj.appmoviles.obratrack.core.domain.model.Relevamiento
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.ObserveObraPublicaUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.relevamientos.ObserveRelevamientosDeObraUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class ObraPublicaDetailUiState(
    val obra: Obra? = null,
    val relevamientos: List<Relevamiento> = emptyList(),
    /** No se pudieron leer los relevamientos: la sección muestra el error, no "vacío". */
    val relevamientosError: DomainError? = null,
    val isLoading: Boolean = true,
    /** No se pudo leer la obra (distinto de "no existe o ya no es pública"). */
    val error: DomainError? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel(assistedFactory = ObraPublicaDetailViewModel.Factory::class)
class ObraPublicaDetailViewModel @AssistedInject constructor(
    @Assisted private val obraId: String,
    private val observeObraPublicaUseCase: ObserveObraPublicaUseCase,
    private val observeRelevamientosUseCase: ObserveRelevamientosDeObraUseCase
) : ViewModel() {

    @dagger.assisted.AssistedFactory
    interface Factory {
        fun create(obraId: String): ObraPublicaDetailViewModel
    }

    /** Cada incremento vuelve a leer la obra y sus relevamientos; solo lo hace "Reintentar". */
    private val reintentos = MutableStateFlow(0)

    val uiState: StateFlow<ObraPublicaDetailUiState> = reintentos
        .flatMapLatest { combinarLecturas() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ObraPublicaDetailUiState(isLoading = true)
        )

    fun reintentar() {
        reintentos.update { it + 1 }
    }

    // Los dos Flows atrapan su error: sin el catch, cerraría la app en vez de avisar.
    private fun combinarLecturas(): Flow<ObraPublicaDetailUiState> = combine(
        observeObraPublicaUseCase(obraId)
            .map { Result.success(it) }
            .catch { e ->
                Log.e(TAG, "No se pudo leer la obra pública", e)
                emit(Result.failure(e))
            },
        observeRelevamientosUseCase(obraId)
            .map { Result.success(it) }
            .catch { e ->
                Log.e(TAG, "No se pudieron leer los relevamientos de la obra pública", e)
                emit(Result.failure(e))
            }
    ) { obra, relevamientos ->
        ObraPublicaDetailUiState(
            obra = obra.getOrNull(),
            relevamientos = relevamientos.getOrNull().orEmpty(),
            relevamientosError = relevamientos.exceptionOrNull()?.aErrorDeLectura(RelevamientoError.Desconocido),
            isLoading = false,
            error = obra.exceptionOrNull()?.aErrorDeLectura()
        )
    }.onStart { emit(ObraPublicaDetailUiState(isLoading = true)) }

    private companion object {
        const val TAG = "ObraPublicaDetailVM"
    }
}
