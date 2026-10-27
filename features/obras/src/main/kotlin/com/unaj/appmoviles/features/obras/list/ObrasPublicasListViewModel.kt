package com.unaj.appmoviles.features.obras.list

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unaj.appmoviles.obratrack.core.domain.error.AppResult
import com.unaj.appmoviles.obratrack.core.domain.error.DomainError
import com.unaj.appmoviles.obratrack.core.domain.error.aErrorDeLectura
import com.unaj.appmoviles.obratrack.core.domain.model.Obra
import com.unaj.appmoviles.obratrack.core.domain.usecase.auth.ObserveSessionUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.ObserveSuscripcionesUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.ObserveObrasPublicasUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.SetSuscripcionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ObrasPublicasListUiState(
    val obras: List<Obra> = emptyList(),
    val suscripciones: Set<String> = emptySet(),
    val suscripcionesPendientes: Set<String> = emptySet(),
    val errorSuscripcion: Boolean = false,
    val isLoading: Boolean = true,
    /** No se pudieron leer las obras: la pantalla muestra el error con "Reintentar", no "vacío". */
    val error: DomainError? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ObrasPublicasListViewModel @Inject constructor(
    private val observeObrasPublicasUseCase: ObserveObrasPublicasUseCase,
    private val observeSession: ObserveSessionUseCase,
    observeSuscripciones: ObserveSuscripcionesUseCase,
    private val setSuscripcion: SetSuscripcionUseCase
) : ViewModel() {

    /** Cada incremento vuelve a leer las obras; solo lo hace el botón "Reintentar". */
    private val reintentos = MutableStateFlow(0)
    private val suscripcionesPendientes = MutableStateFlow(emptySet<String>())
    private val errorSuscripcion = MutableStateFlow(false)

    /** Se arma en cada lectura (no una vez): así "Reintentar" vuelve a consultar. */
    private fun leerObras() = observeObrasPublicasUseCase()
        .map { Result.success(it) }
        // Sin el catch, el error del Flow (por ejemplo, sin permisos) cerraría la app.
        .catch { e ->
            Log.e(TAG, "No se pudieron leer las obras públicas", e)
            emit(Result.failure(e))
        }

    private val suscripciones = observeSession()
        .flatMapLatest { user ->
            if (user == null) flowOf(emptySet()) else observeSuscripciones(user.id)
        }
        .catch { emit(emptySet()) }

    val uiState: StateFlow<ObrasPublicasListUiState> = reintentos
        .flatMapLatest {
            combine(leerObras(), suscripciones, suscripcionesPendientes, errorSuscripcion) { lectura, seguidas, pendientes, errorAlSeguir ->
                ObrasPublicasListUiState(
                    obras = lectura.getOrNull().orEmpty(),
                    suscripciones = seguidas,
                    suscripcionesPendientes = pendientes,
                    errorSuscripcion = errorAlSeguir,
                    isLoading = false,
                    error = lectura.exceptionOrNull()?.aErrorDeLectura()
                )
            }.onStart { emit(ObrasPublicasListUiState(isLoading = true)) }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ObrasPublicasListUiState(isLoading = true)
        )

    fun reintentar() {
        reintentos.update { it + 1 }
    }

    fun toggleSuscripcion(obraId: String) {
        if (obraId in suscripcionesPendientes.value) return
        viewModelScope.launch {
            val userId = observeSession().first()?.id ?: return@launch
            val seguir = obraId !in uiState.value.suscripciones
            suscripcionesPendientes.update { it + obraId }
            errorSuscripcion.value = false
            val result = setSuscripcion(userId, obraId, seguir)
            suscripcionesPendientes.update { it - obraId }
            errorSuscripcion.value = result is AppResult.Error
        }
    }

    private companion object {
        const val TAG = "ObrasPublicasListVM"
    }
}
