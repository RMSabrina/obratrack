package com.unaj.appmoviles.obratrack.core.domain.usecase

import com.unaj.appmoviles.obratrack.core.domain.model.MotivoFalloEscritura
import com.unaj.appmoviles.obratrack.core.domain.repository.FallosEscrituraRepository
import com.unaj.appmoviles.obratrack.core.domain.usecase.sync.ObserveFallosEscrituraUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class ObserveFallosEscrituraUseCaseTest {

    @Test
    fun emiteLosFallosDelRepositorio() = runBlocking {
        val repo = object : FallosEscrituraRepository {
            override val fallos: Flow<MotivoFalloEscritura> =
                flowOf(MotivoFalloEscritura.SIN_PERMISO, MotivoFalloEscritura.OTRO)
        }

        val fallos = ObserveFallosEscrituraUseCase(repo)().toList()

        assertEquals(listOf(MotivoFalloEscritura.SIN_PERMISO, MotivoFalloEscritura.OTRO), fallos)
    }
}
