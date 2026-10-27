package com.unaj.appmoviles.obratrack.navigation

import androidx.lifecycle.ViewModel
import com.unaj.appmoviles.obratrack.core.domain.model.MotivoFalloEscritura
import com.unaj.appmoviles.obratrack.core.domain.usecase.sync.ObserveFallosEscrituraUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class FallosEscrituraViewModel @Inject constructor(
    observeFallos: ObserveFallosEscrituraUseCase
) : ViewModel() {
    val fallos: Flow<MotivoFalloEscritura> = observeFallos()
}
