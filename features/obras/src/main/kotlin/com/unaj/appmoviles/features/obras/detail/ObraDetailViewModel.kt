package com.unaj.appmoviles.features.obras.detail

import android.net.Uri
import com.unaj.appmoviles.features.obras.export.EstadoPdf
import com.unaj.appmoviles.features.obras.export.GeneradorPdf
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unaj.appmoviles.obratrack.core.domain.error.AppResult
import com.unaj.appmoviles.obratrack.core.domain.error.ObraError
import com.unaj.appmoviles.obratrack.core.domain.error.RelevamientoError
import com.unaj.appmoviles.obratrack.core.domain.error.aErrorDeLectura
import com.unaj.appmoviles.obratrack.core.domain.model.Relevamiento
import com.unaj.appmoviles.obratrack.core.domain.model.Tarea
import com.unaj.appmoviles.obratrack.core.domain.usecase.auth.ObserveSessionUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.sync.ObservePendientesUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.DeleteObraUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.EstadoFechaVisita
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.EstadoVisitaConHora
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.ObserveObraUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.ObtenerPronosticoVisitaUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.estadoFechaVisita
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.estadoVisitaConHora
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.toFechaCalendario
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import java.time.Clock
import java.time.LocalDate

import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel(assistedFactory = ObraDetailViewModel.Factory::class)
class ObraDetailViewModel @AssistedInject constructor(
    @Assisted private val obraId: String,
    observeSession: ObserveSessionUseCase,
    private val observeObra: ObserveObraUseCase,
    private val observeRelevamientos: ObserveRelevamientosUseCase,
    private val observeTareas: ObserveTareasUseCase,
    observePendientes: ObservePendientesUseCase,
    private val deleteObraUseCase: DeleteObraUseCase,
    private val obtenerPronosticoVisita: ObtenerPronosticoVisitaUseCase,
    private val clock: Clock
) : ViewModel() {

    private val generadorPdf = GeneradorPdf<Uri>(viewModelScope)
    val pdf: StateFlow<EstadoPdf<Uri>> = generadorPdf.estado

    /** [trabajo] arma el PDF (fuera del hilo principal) y devuelve su `Uri`, o null si falló. */
    fun generarPdf(trabajo: suspend () -> Uri?) = generadorPdf.generar(trabajo)

    fun pdfConsumido() = generadorPdf.consumido()

    @dagger.assisted.AssistedFactory
    interface Factory {
        fun create(obraId: String): ObraDetailViewModel
    }

    private val _uiState = MutableStateFlow(ObraDetailUiState())
    val uiState: StateFlow<ObraDetailUiState> = _uiState.asStateFlow()
    private var pronosticoJob: Job? = null
    private var clavePronostico: String? = null

    /** Cada incremento vuelve a leer la obra, relevamientos y tareas; solo lo hace "Reintentar". */
    private val reintentos = MutableStateFlow(0)

    init {
        viewModelScope.launch {
            observePendientes.relevamientos(obraId).collect { ids ->
                _uiState.update { it.copy(relevamientosPendientes = ids) }
            }
        }
        viewModelScope.launch {
            observeSession().combine(reintentos) { user, _ -> user }.collectLatest { user ->
                if (user == null) {
                    pronosticoJob?.cancel()
                    clavePronostico = null
                    _uiState.value = ObraDetailUiState(isLoading = false, error = ObraError.NoAutorizada)
                    return@collectLatest
                }
                combine(
                    observeObra(user.id, obraId),
                    observeRelevamientosOError(user.id),
                    observeTareasOError(user.id)
                ) { obra, relevamientos, tareas ->
                    val esDueno = obra?.userId == user.id
                    // Si no se pudieron leer las tareas, el resumen se oculta (la falla ya quedó en el log).
                    val resumenTareas = if (esDueno) tareas.getOrNull()?.resumen(LocalDate.now(clock)) else null
                    val proximaVisitaAtrasada = obra?.proximaVisita?.let { proxima ->
                        val fecha = proxima.toFechaCalendario()
                        val hora = obra.horaVisita
                        if (hora != null) {
                            estadoVisitaConHora(fecha, hora, clock) is EstadoVisitaConHora.Atrasada
                        } else {
                            estadoFechaVisita(fecha, LocalDate.now(clock)) is EstadoFechaVisita.Atrasada
                        }
                    } ?: false
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
                            relevamientos = relevamientos.getOrNull().orEmpty(),
                            relevamientosError = relevamientos.exceptionOrNull()?.aErrorDeLectura(RelevamientoError.Desconocido),
                            tareas = if (esDueno) tareas.getOrNull().orEmpty() else emptyList(),
                            resumenTareas = resumenTareas,
                            proximaVisitaAtrasada = proximaVisitaAtrasada,
                            error = if (obra == null) ObraError.NoEncontrada else null,
                            isDeleting = current.isDeleting,
                            isDeleted = current.isDeleted,
                            deleteError = current.deleteError
                        )
                    }
                }.catch { e ->
                    Log.e(TAG, "No se pudo leer la obra", e)
                    if (!_uiState.value.isDeleting) {
                        _uiState.update { it.copy(isLoading = false, error = e.aErrorDeLectura()) }
                    }
                }.collect { newState ->
                    val sameVisit = mismoContextoVisita(newState, _uiState.value)
                    _uiState.value = newState.copy(
                        pronosticoVisita = _uiState.value.pronosticoVisita.takeIf { sameVisit },
                        relevamientosPendientes = _uiState.value.relevamientosPendientes
                    )
                    actualizarPronostico(newState)
                }
            }
        }
    }

    fun reintentar() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        reintentos.update { it + 1 }
    }

    private fun actualizarPronostico(state: ObraDetailUiState) {
        val obra = state.obra
        val fecha = obra?.proximaVisita?.toFechaCalendario()
        val ubicacion = obra?.ubicacion
        val clave = clavePronostico(state)
        if (clave == clavePronostico) return

        clavePronostico = clave
        pronosticoJob?.cancel()
        if (clave == null) {
            _uiState.update {
                it.copy(
                    pronosticoVisita = if (state.isEditable && fecha != null && !state.proximaVisitaAtrasada) {
                        if (ubicacion == null) PronosticoUiState.SinUbicacion else null
                    } else null
                )
            }
            return
        }

        val visitaFecha = checkNotNull(fecha)
        val visitaUbicacion = checkNotNull(ubicacion)
        val hoy = LocalDate.now(clock)
        if (visitaFecha.isAfter(hoy.plusDays(15))) {
            _uiState.update { it.copy(pronosticoVisita = PronosticoUiState.FueraDeRango) }
            return
        }

        _uiState.update { it.copy(pronosticoVisita = PronosticoUiState.Cargando) }
        pronosticoJob = viewModelScope.launch {
            val resultado = try {
                PronosticoUiState.Disponible(
                    obtenerPronosticoVisita(
                    latitud = visitaUbicacion.latitud,
                    longitud = visitaUbicacion.longitud,
                    fecha = visitaFecha,
                    hora = obra.horaVisita
                    )
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                PronosticoUiState.SinDatos
            }
            if (clavePronostico == clave) {
                _uiState.update { it.copy(pronosticoVisita = resultado) }
            }
        }
    }

    private fun clavePronostico(state: ObraDetailUiState): String? {
        val obra = state.obra ?: return null
        val fecha = obra.proximaVisita?.toFechaCalendario() ?: return null
        val ubicacion = obra.ubicacion ?: return null
        if (!state.isEditable || state.proximaVisitaAtrasada) return null
        return "${obra.id}|$fecha|${obra.horaVisita}|${ubicacion.latitud}|${ubicacion.longitud}"
    }

    private fun mismoContextoVisita(a: ObraDetailUiState, b: ObraDetailUiState): Boolean =
        a.isEditable == b.isEditable &&
            a.proximaVisitaAtrasada == b.proximaVisitaAtrasada &&
            a.obra?.id == b.obra?.id &&
            a.obra?.proximaVisita == b.obra?.proximaVisita &&
            a.obra?.horaVisita == b.obra?.horaVisita &&
            a.obra?.ubicacion == b.obra?.ubicacion

    fun reintentarPronostico() {
        val state = _uiState.value
        if (state.pronosticoVisita != PronosticoUiState.SinDatos) return
        clavePronostico = null
        actualizarPronostico(state)
    }

    /**
     * Result.failure si fallan los relevamientos: la obra se sigue mostrando y la sección avisa
     * del error (si no, el error cortaría el combine y la lista quedaría vacía como si no
     * hubiera ninguno).
     */
    private fun observeRelevamientosOError(userId: String): Flow<Result<List<Relevamiento>>> =
        observeRelevamientos(userId, obraId)
            .map { Result.success(it) }
            .catch { e ->
                Log.e(TAG, "No se pudieron leer los relevamientos", e)
                emit(Result.failure(e))
            }

    /**
     * Las tareas son privadas: solo se observan si el usuario es dueño (si no, Firestore
     * rechaza la lectura). Un error de tareas devuelve Result.failure (se oculta el resumen)
     * sin afectar al resto.
     */
    private fun observeTareasOError(userId: String): Flow<Result<List<Tarea>>> =
        observeObra(userId, obraId)
            .map { obra -> obra?.userId == userId }
            .distinctUntilChanged()
            .flatMapLatest { esDueno ->
                if (!esDueno) {
                    flowOf<Result<List<Tarea>>>(Result.success(emptyList()))
                } else {
                    observeTareas(obraId)
                        .map { Result.success(it) }
                        .catch { e ->
                            Log.e(TAG, "No se pudieron leer las tareas", e)
                            emit(Result.failure(e))
                        }
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

    private companion object {
        const val TAG = "ObraDetailViewModel"
    }
}
