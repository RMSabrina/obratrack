package com.unaj.appmoviles.features.obras.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unaj.appmoviles.obratrack.core.domain.error.AppResult
import com.unaj.appmoviles.obratrack.core.domain.error.ObraError
import com.unaj.appmoviles.obratrack.core.domain.model.Relevamiento
import com.unaj.appmoviles.obratrack.core.domain.model.ResumenTareas
import com.unaj.appmoviles.obratrack.core.domain.model.Tarea
import com.unaj.appmoviles.obratrack.core.domain.usecase.auth.ObserveSessionUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.DeleteObraUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.ObserveObraUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.relevamientos.ObserveRelevamientosUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.tareas.ObserveTareasUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.tareas.resumen
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate

import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import com.unaj.appmoviles.obratrack.core.domain.model.Colaborador
import com.unaj.appmoviles.obratrack.core.domain.model.GravedadIncidente
import com.unaj.appmoviles.obratrack.core.domain.model.Incidente
import com.unaj.appmoviles.obratrack.core.domain.model.NotificacionItem
import com.unaj.appmoviles.obratrack.core.domain.model.TipoNotificacion
import com.unaj.appmoviles.obratrack.core.domain.model.User
import com.unaj.appmoviles.obratrack.core.domain.model.UserRole
import com.unaj.appmoviles.obratrack.core.domain.repository.ColaboradorRepository
import com.unaj.appmoviles.obratrack.core.domain.repository.IncidenteRepository
import com.unaj.appmoviles.obratrack.core.domain.repository.NotificacionRepository
import dagger.hilt.android.lifecycle.HiltViewModel

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel(assistedFactory = ObraDetailViewModel.Factory::class)
class ObraDetailViewModel @AssistedInject constructor(
    @Assisted private val obraId: String,
    observeSession: ObserveSessionUseCase,
    private val observeObra: ObserveObraUseCase,
    private val observeRelevamientos: ObserveRelevamientosUseCase,
    private val observeTareas: ObserveTareasUseCase,
    private val deleteObraUseCase: DeleteObraUseCase,
    private val colaboradorRepository: ColaboradorRepository,
    private val incidenteRepository: IncidenteRepository,
    private val notificacionRepository: NotificacionRepository,
    private val clock: Clock
) : ViewModel() {

    @dagger.assisted.AssistedFactory
    interface Factory {
        fun create(obraId: String): ObraDetailViewModel
    }

    private val _uiState = MutableStateFlow(ObraDetailUiState())
    val uiState: StateFlow<ObraDetailUiState> = _uiState.asStateFlow()
    private var currentUser: User? = null

    init {
        viewModelScope.launch {
            observeSession().collectLatest { user ->
                currentUser = user
                if (user == null) {
                    _uiState.update { it.copy(isLoading = false, error = ObraError.NoAutorizada) }
                    return@collectLatest
                }
                combine(
                    observeObra(user.id, obraId),
                    observeRelevamientosOError(user.id),
                    observeTareasOError(user.id),
                    colaboradorRepository.observeColaboradores(obraId).catch { emit(emptyList()) },
                    incidenteRepository.observeIncidentes(obraId).catch { emit(emptyList()) }
                ) { obra, relevamientos, tareas, colaboradores, incidentes ->
                    val esDueno = obra?.userId == user.id
                    val resumenTareas = if (esDueno && tareas.isNotEmpty()) tareas.resumen(LocalDate.now(clock)) else if (esDueno) ResumenTareas(0, 0, 0) else null
                    val current = _uiState.value
                    if (current.isDeleting) {
                        if (obra == null && current.deleteError == ObraError.Timeout) {
                            current.copy(isDeleted = true)
                        } else {
                            current
                        }
                    } else {
                        ObraDetailUiState(
                            isLoading = false,
                            isEditable = esDueno,
                            obra = obra,
                            relevamientos = relevamientos.orEmpty(),
                            relevamientosError = relevamientos == null,
                            tareas = if (esDueno) tareas else emptyList(),
                            resumenTareas = resumenTareas,
                            colaboradores = colaboradores,
                            incidentes = incidentes,
                            error = if (obra == null) ObraError.NoEncontrada else null,
                            isDeleting = current.isDeleting,
                            isDeleted = current.isDeleted,
                            deleteError = current.deleteError
                        )
                    }
                }.catch {
                    if (!_uiState.value.isDeleting) {
                        _uiState.update { it.copy(isLoading = false, error = ObraError.Desconocido) }
                    }
                }.collect { newState ->
                    _uiState.value = newState
                }
            }
        }
    }

    /**
     * null si fallan los relevamientos: la obra se sigue mostrando y la sección avisa del error
     * (si no, el error cortaría el combine y la lista quedaría vacía como si no hubiera ninguno).
     */
    private fun observeRelevamientosOError(userId: String): Flow<List<Relevamiento>?> =
        observeRelevamientos(userId, obraId)
            .map<List<Relevamiento>, List<Relevamiento>?> { it }
            .catch { emit(null) }

    /**
     * Las tareas son privadas: solo se observan si el usuario es dueño (si no, Firestore
     * rechaza la lectura). Un error de tareas devuelve lista vacía sin afectar al resto.
     */
    private fun observeTareasOError(userId: String): Flow<List<Tarea>> =
        observeObra(userId, obraId)
            .map { obra -> obra?.userId == userId }
            .distinctUntilChanged()
            .flatMapLatest { esDueno ->
                if (!esDueno) {
                    flowOf(emptyList())
                } else {
                    observeTareas(obraId)
                        .catch { emit(emptyList()) }
                }
            }

    fun deleteObra() {
        if (_uiState.value.isDeleting) return
        _uiState.update { it.copy(isDeleting = true, deleteError = null) }
        viewModelScope.launch {
            when (val result = deleteObraUseCase(obraId)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(isDeleting = true, isDeleted = true) }
                }
                is AppResult.Error -> {
                    if (result.error == ObraError.Timeout) {
                        if (_uiState.value.obra == null) {
                            _uiState.update { it.copy(isDeleting = true, isDeleted = true) }
                        } else {
                            _uiState.update { it.copy(isDeleting = false, deleteError = ObraError.Timeout) }
                        }
                    } else {
                        _uiState.update { it.copy(isDeleting = false, deleteError = result.error) }
                    }
                }
            }
        }
    }

    fun onDeleteErrorDismissed() {
        _uiState.update { it.copy(deleteError = null) }
    }

    fun agregarColaborador(email: String, nombre: String, rol: UserRole) {
        viewModelScope.launch {
            colaboradorRepository.agregarColaborador(
                obraId = obraId,
                email = email,
                nombre = nombre,
                rol = rol
            )
        }
    }

    fun removerColaborador(colaboradorId: String) {
        viewModelScope.launch {
            colaboradorRepository.removerColaborador(obraId, colaboradorId)
        }
    }

    fun reportarIncidente(titulo: String, descripcion: String, gravedad: GravedadIncidente) {
        viewModelScope.launch {
            val user = currentUser ?: return@launch
            val incidente = Incidente(
                id = "",
                obraId = obraId,
                titulo = titulo,
                descripcion = descripcion,
                gravedad = gravedad,
                reportadoPorUserId = user.id,
                reportadoPorNombre = user.name.ifBlank { "Inspector" },
                fecha = System.currentTimeMillis(),
                resuelto = false
            )
            incidenteRepository.reportarIncidente(incidente)

            val notif = NotificacionItem(
                id = "",
                userId = user.id,
                obraId = obraId,
                titulo = "⚠️ ${incidente.titulo}",
                mensaje = incidente.descripcion,
                tipo = TipoNotificacion.INCIDENTE_REPORTADO,
                fecha = System.currentTimeMillis(),
                leida = false
            )
            notificacionRepository.crearNotificacion(notif)
        }
    }

    fun resolverIncidente(incidenteId: String) {
        viewModelScope.launch {
            incidenteRepository.resolverIncidente(obraId, incidenteId)
        }
    }
}
