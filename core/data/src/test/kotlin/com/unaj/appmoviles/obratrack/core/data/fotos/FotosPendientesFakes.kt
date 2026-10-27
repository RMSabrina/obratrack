package com.unaj.appmoviles.obratrack.core.data.fotos

import com.unaj.appmoviles.obratrack.core.domain.error.AppResult
import com.unaj.appmoviles.obratrack.core.domain.error.RelevamientoError
import com.unaj.appmoviles.obratrack.core.domain.repository.FotoRelevamientoRepository
import com.unaj.appmoviles.obratrack.core.domain.repository.ImageStorageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakePlanificador : PlanificadorSubidas {
    val activas = MutableStateFlow<Set<SubidaProgramada>>(emptySet())
    val programaciones = mutableListOf<SubidaProgramada>()
    val canceladas = mutableListOf<String>()
    val obrasCanceladas = mutableListOf<String>()

    override val programadas: Flow<Set<SubidaProgramada>> = activas

    override fun programar(obraId: String, relevamientoId: String) {
        val subida = SubidaProgramada(obraId, relevamientoId)
        programaciones += subida
        // Como REPLACE: la subida nueva reemplaza a la anterior del mismo relevamiento.
        activas.value = activas.value.filter { it.relevamientoId != relevamientoId }.toSet() + subida
    }

    override fun cancelar(relevamientoId: String) {
        canceladas += relevamientoId
        activas.value = activas.value.filter { it.relevamientoId != relevamientoId }.toSet()
    }

    override fun cancelarDeObra(obraId: String) {
        obrasCanceladas += obraId
        activas.value = activas.value.filter { it.obraId != obraId }.toSet()
    }
}

class FakeImagenes(var resultado: AppResult<String> = AppResult.Success("https://img/nueva")) : ImageStorageRepository {
    val nombres = mutableListOf<String>()
    var llamadas = 0

    override suspend fun uploadImage(bytes: ByteArray, folder: String, name: String): AppResult<String> {
        llamadas++
        nombres += name
        return resultado
    }
}

class FakeFotoRelevamientos(
    var existe: AppResult<Boolean> = AppResult.Success(true),
    var asignar: AppResult<Boolean> = AppResult.Success(true)
) : FotoRelevamientoRepository {
    val asignadas = mutableListOf<Triple<String, String, String>>()

    override suspend fun existe(obraId: String, relevamientoId: String): AppResult<Boolean> = existe

    override suspend fun asignarFotoUrl(obraId: String, relevamientoId: String, url: String): AppResult<Boolean> {
        asignadas += Triple(obraId, relevamientoId, url)
        return asignar
    }
}

val SIN_RED: AppResult<Nothing> = AppResult.Error(RelevamientoError.Red)
