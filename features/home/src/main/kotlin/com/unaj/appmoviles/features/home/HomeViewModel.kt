package com.unaj.appmoviles.features.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unaj.appmoviles.obratrack.core.domain.usecase.auth.ObserveSessionUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.ObserveObrasUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.relevamientos.ObserveAllRelevamientosUseCase
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val observeSessionUseCase: ObserveSessionUseCase,
    private val observeObrasUseCase: ObserveObrasUseCase,
    private val observeAllRelevamientosUseCase: ObserveAllRelevamientosUseCase
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<HomeUiState> = observeSessionUseCase()
        .flatMapLatest { user ->
            if (user == null) {
                flowOf(HomeUiState(isLoading = false))
            } else {
                combine(
                    observeObrasUseCase(user.id),
                    observeAllRelevamientosUseCase(user.id).catch { e ->
                        Log.e("HomeViewModel", "Fallo al obtener relevamientos. \u00bfFalta \u00edndice de collectionGroup?", e)
                        emit(emptyList())
                    }
                ) { obras, relevamientos ->
                    Log.d("HomeViewModel", "obras: ${obras.size}, relevamientos: ${relevamientos.size}")
                    val recientes = relevamientos
                        .sortedByDescending { it.fecha }
                        .take(3)
                        .map { rel ->
                            val obra = obras.find { it.id == rel.obraId }
                            RelevamientoReciente(
                                relevamiento = rel,
                                obraNombre = obra?.nombre ?: "Obra desconocida"
                            )
                        }
                        
                    HomeUiState(
                        isLoading = false,
                        userName = user.name.ifBlank { "Usuario" },
                        obras = obras,
                        recientes = recientes
                    )
                }.catch { e ->
                    Log.e("HomeViewModel", "Error general en Home", e)
                    emit(HomeUiState(isLoading = false, userName = user.name.ifBlank { "Usuario" }, error = null)) // O un error m\u00e1s espec\u00edfico
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState(isLoading = true)
        )
}