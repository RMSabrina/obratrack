package com.unaj.appmoviles.features.obras.form

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unaj.appmoviles.obratrack.core.domain.error.AppResult
import com.unaj.appmoviles.obratrack.core.domain.error.ObraError
import com.unaj.appmoviles.obratrack.core.domain.model.ObraDraft
import com.unaj.appmoviles.obratrack.core.domain.model.Ubicacion
import com.unaj.appmoviles.obratrack.core.domain.usecase.location.GetAddressFromUbicacionUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.location.GetUbicacionFromAddressUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.CreateObraUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.auth.ObserveSessionUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.UpdateObraUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel

@HiltViewModel(assistedFactory = ObraFormViewModel.Factory::class)
class ObraFormViewModel @AssistedInject constructor(
    private val observeSession: ObserveSessionUseCase,
    private val createObra: CreateObraUseCase,
    private val updateObra: UpdateObraUseCase,
    private val getUbicacionFromAddress: GetUbicacionFromAddressUseCase,
    private val getAddressFromUbicacion: GetAddressFromUbicacionUseCase,
    @Assisted private val obraId: String?
) : ViewModel() {

    @dagger.assisted.AssistedFactory
    interface Factory {
        fun create(obraId: String?): ObraFormViewModel
    }

    private val _uiState = MutableStateFlow<ObraFormUiState>(ObraFormUiState.Idle)
    val uiState: StateFlow<ObraFormUiState> = _uiState.asStateFlow()

    fun save(draft: ObraDraft) {
        if (_uiState.value is ObraFormUiState.Loading) return
        _uiState.value = ObraFormUiState.Loading
        viewModelScope.launch {
            val user = observeSession().first()
            if (user == null) {
                _uiState.value = ObraFormUiState.Error(ObraError.NoAutorizada)
                return@launch
            }
            val result = if (obraId == null) {
                createObra(user.id, draft)
            } else {
                updateObra(user.id, obraId, draft)
            }
            _uiState.value = when (result) {
                is AppResult.Success -> ObraFormUiState.Saved
                is AppResult.Error -> ObraFormUiState.Error(result.error)
            }
        }
    }

    fun onErrorShown() {
        if (_uiState.value is ObraFormUiState.Error) _uiState.value = ObraFormUiState.Idle
    }

    suspend fun buscarUbicacionDeDireccion(direccion: String): Ubicacion? {
        return getUbicacionFromAddress(direccion)
    }

    suspend fun buscarDireccionDeUbicacion(latitud: Double, longitud: Double): String? {
        return getAddressFromUbicacion(latitud, longitud)
    }
}
