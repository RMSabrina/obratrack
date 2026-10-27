package com.unaj.appmoviles.obratrack.core.domain.repository

import com.unaj.appmoviles.obratrack.core.domain.model.MotivoFalloEscritura
import kotlinx.coroutines.flow.Flow

interface FallosEscrituraRepository {
    /** Cambios que se guardaron sin esperar al servidor y este rechazó al sincronizar. */
    val fallos: Flow<MotivoFalloEscritura>
}
