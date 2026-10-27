package com.unaj.appmoviles.obratrack.core.domain.usecase

import com.unaj.appmoviles.obratrack.core.domain.repository.ConexionRepository
import com.unaj.appmoviles.obratrack.core.domain.repository.PendientesRepository
import com.unaj.appmoviles.obratrack.core.domain.usecase.sync.ObserveConexionUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.sync.ObservePendientesUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class SincronizacionUseCasesTest {

    @Test
    fun pendientes_pasaLosIdsDeCadaColeccionConSuClave() = runBlocking {
        val repo = object : PendientesRepository {
            override fun obras(userId: String): Flow<Set<String>> = flowOf(setOf("obra-de-$userId"))
            override fun relevamientos(obraId: String): Flow<Set<String>> = flowOf(setOf("rel-de-$obraId"))
            override fun tareas(obraId: String): Flow<Set<String>> = flowOf(setOf("tarea-de-$obraId"))
        }
        val useCase = ObservePendientesUseCase(repo)

        assertEquals(listOf(setOf("obra-de-u1")), useCase.obras("u1").toList())
        assertEquals(listOf(setOf("rel-de-o1")), useCase.relevamientos("o1").toList())
        assertEquals(listOf(setOf("tarea-de-o1")), useCase.tareas("o1").toList())
    }

    @Test
    fun conexion_emiteLosCambiosDelRepositorio() = runBlocking {
        val repo = object : ConexionRepository {
            override val enLinea: Flow<Boolean> = flowOf(true, false, true)
        }

        assertEquals(listOf(true, false, true), ObserveConexionUseCase(repo)().toList())
    }
}
