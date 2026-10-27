package com.unaj.appmoviles.obratrack.core.domain.repository

import kotlinx.coroutines.flow.Flow

interface ConexionRepository {
    /** true si el teléfono tiene una red con internet; emite solo cuando cambia. */
    val enLinea: Flow<Boolean>
}
