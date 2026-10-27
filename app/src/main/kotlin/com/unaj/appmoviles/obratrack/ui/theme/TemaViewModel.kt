package com.unaj.appmoviles.obratrack.ui.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unaj.appmoviles.obratrack.core.domain.model.ModoTema
import com.unaj.appmoviles.obratrack.core.domain.repository.ThemePreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Tema elegido. Arranca con el valor guardado (lectura síncrona), así el primer cuadro ya sale con el
 * tema correcto y no hay un destello con el equivocado.
 */
@HiltViewModel
class TemaViewModel @Inject constructor(
    repositorio: ThemePreferencesRepository
) : ViewModel() {

    val modo: StateFlow<ModoTema> = repositorio.observeModo()
        .stateIn(viewModelScope, SharingStarted.Eagerly, repositorio.getModo())
}
