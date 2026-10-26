package com.unaj.appmoviles.obratrack.core.domain.usecase

import com.unaj.appmoviles.obratrack.core.domain.error.AppResult
import com.unaj.appmoviles.obratrack.core.domain.error.ObraError
import com.unaj.appmoviles.obratrack.core.domain.error.RelevamientoError
import com.unaj.appmoviles.obratrack.core.domain.model.EstadoObra
import com.unaj.appmoviles.obratrack.core.domain.model.Obra
import com.unaj.appmoviles.obratrack.core.domain.model.Relevamiento
import com.unaj.appmoviles.obratrack.core.domain.model.RelevamientoDraft
import com.unaj.appmoviles.obratrack.core.domain.repository.RelevamientoRepository
import com.unaj.appmoviles.obratrack.core.domain.usecase.relevamientos.DeleteRelevamientoUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.relevamientos.ProximaVisitaObra
import com.unaj.appmoviles.obratrack.core.domain.usecase.relevamientos.SincronizarProximaVisitaObraUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.relevamientos.UpdateRelevamientoUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.relevamientos.proximaVisitaDeObra
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Obra y relevamientos en memoria; [guardar] imita lo que hace el data source de Firestore al crear. */
private class Almacen {
    var obra = Obra(
        id = "obra-1", userId = "uid", nombre = "Casa", cliente = "Cliente", direccion = "Calle 1",
        publica = false, estado = EstadoObra.ACTIVA, fechaInicio = 1L, ultimaVisita = null
    )
    val relevamientos = mutableListOf<Relevamiento>()
    var setProximaVisitaCalls = 0
    var errorAlLeer: RelevamientoError? = null
    var errorAlBorrar: RelevamientoError? = null
    var errorAlEscribirObra: ObraError? = null

    fun guardar(relevamiento: Relevamiento) {
        relevamientos += relevamiento
        if (relevamiento.proximaVisita != null) {
            obra = obra.copy(proximaVisita = relevamiento.proximaVisita, horaVisita = relevamiento.horaVisita)
        }
    }
}

private class RelevamientoRepositoryEnMemoria(private val almacen: Almacen) : RelevamientoRepository {
    override fun observeAll(userId: String): Flow<List<Relevamiento>> = emptyFlow()
    override fun observe(userId: String, obraId: String): Flow<List<Relevamiento>> = emptyFlow()
    override fun observeByObra(obraId: String): Flow<List<Relevamiento>> = emptyFlow()
    override fun observeByTarea(obraId: String, tareaId: String): Flow<List<Relevamiento>> = emptyFlow()
    override suspend fun create(
        userId: String, obraId: String, draft: RelevamientoDraft, photo: ByteArray?
    ): AppResult<Relevamiento> = TODO()

    /** Como el data source: la próxima visita se copia a la obra solo si el relevamiento la trae. */
    override suspend fun update(
        userId: String, obraId: String, relevamientoId: String, draft: RelevamientoDraft, photo: ByteArray?
    ): AppResult<Relevamiento> {
        val actualizado = Relevamiento(
            id = relevamientoId, obraId = obraId, userId = userId, fecha = draft.fecha, nota = draft.nota,
            proximaVisita = draft.proximaVisita, horaVisita = draft.horaVisita
        )
        almacen.relevamientos.removeAll { it.id == relevamientoId }
        almacen.guardar(actualizado)
        return AppResult.Success(actualizado)
    }

    override suspend fun getByObra(obraId: String): AppResult<List<Relevamiento>> =
        almacen.errorAlLeer?.let { AppResult.Error(it) } ?: AppResult.Success(almacen.relevamientos.toList())

    override suspend fun delete(obraId: String, relevamientoId: String): AppResult<Unit> {
        almacen.errorAlBorrar?.let { return AppResult.Error(it) }
        almacen.relevamientos.removeAll { it.id == relevamientoId }
        return AppResult.Success(Unit)
    }

    override suspend fun deleteAllByObra(obraId: String): AppResult<Unit> = TODO()
}

private class ObraRepositoryEnMemoria(private val almacen: Almacen) : FakeBaseObraRepository() {
    override suspend fun setProximaVisita(obraId: String, proximaVisita: Long?, horaVisita: Int?): AppResult<Unit> {
        almacen.setProximaVisitaCalls++
        almacen.errorAlEscribirObra?.let { return AppResult.Error(it) }
        almacen.obra = almacen.obra.copy(proximaVisita = proximaVisita, horaVisita = horaVisita)
        return AppResult.Success(Unit)
    }
}

class ProximaVisitaObraSyncTest {

    private val almacen = Almacen()
    private val relevamientoRepo = RelevamientoRepositoryEnMemoria(almacen)
    private val sincronizar = SincronizarProximaVisitaObraUseCase(relevamientoRepo, ObraRepositoryEnMemoria(almacen))
    private val borrar = DeleteRelevamientoUseCase(relevamientoRepo, sincronizar)
    private val actualizar = UpdateRelevamientoUseCase(relevamientoRepo, sincronizar)

    private fun relevamiento(id: String, fecha: Long, proxima: Long?, hora: Int? = null) = Relevamiento(
        id = id, obraId = "obra-1", userId = "uid", fecha = fecha, nota = "Nota",
        proximaVisita = proxima, horaVisita = hora
    )

    // ---- Función pura: qué relevamiento define la próxima visita

    @Test
    fun proximaVisitaDeObra_sinRelevamientos_esNull() {
        assertNull(proximaVisitaDeObra(emptyList()))
    }

    @Test
    fun proximaVisitaDeObra_ningunoConFecha_esNull() {
        assertNull(proximaVisitaDeObra(listOf(relevamiento("a", 100, null), relevamiento("b", 200, null))))
    }

    @Test
    fun proximaVisitaDeObra_unSoloRelevamiento_usaSuFechaYHora() {
        val resultado = proximaVisitaDeObra(listOf(relevamiento("a", 100, 5_000L, hora = 600)))

        assertEquals(ProximaVisitaObra(fecha = 5_000L, hora = 600), resultado)
    }

    @Test
    fun proximaVisitaDeObra_variosConFechasDistintas_gana_elDeFechaMasReciente() {
        val resultado = proximaVisitaDeObra(
            listOf(
                relevamiento("viejo", fecha = 100, proxima = 9_000L, hora = 480),
                relevamiento("reciente", fecha = 300, proxima = 4_000L, hora = 900),
                relevamiento("medio", fecha = 200, proxima = 7_000L)
            )
        )

        assertEquals(ProximaVisitaObra(fecha = 4_000L, hora = 900), resultado)
    }

    @Test
    fun proximaVisitaDeObra_ignoraLosRelevamientosSinProximaVisita() {
        val resultado = proximaVisitaDeObra(
            listOf(
                relevamiento("con", fecha = 100, proxima = 5_000L),
                relevamiento("sin", fecha = 900, proxima = null)
            )
        )

        assertEquals(ProximaVisitaObra(fecha = 5_000L, hora = null), resultado)
    }

    @Test
    fun proximaVisitaDeObra_mismaFecha_gana_laProximaVisitaMasLejana() {
        val resultado = proximaVisitaDeObra(
            listOf(relevamiento("a", 100, 5_000L, hora = 60), relevamiento("b", 100, 8_000L, hora = 120))
        )

        assertEquals(ProximaVisitaObra(fecha = 8_000L, hora = 120), resultado)
    }

    @Test
    fun proximaVisitaDeObra_horaInvalida_cuentaComoSinHora() {
        assertEquals(
            ProximaVisitaObra(fecha = 5_000L, hora = null),
            proximaVisitaDeObra(listOf(relevamiento("a", 100, 5_000L, hora = 5000)))
        )
    }

    // ---- Borrar un relevamiento

    @Test
    fun borrarElUnicoRelevamientoConProximaVisita_dejaLaObraSinProximaVisitaNiHora() = runBlocking {
        almacen.guardar(relevamiento("r1", fecha = 100L, proxima = 5_000L, hora = 15 * 60))

        val resultado = borrar("obra-1", "r1")

        assertTrue(resultado is AppResult.Success)
        assertNull(almacen.obra.proximaVisita)
        assertNull(almacen.obra.horaVisita)
    }

    @Test
    fun borrarElQueDefineLaProximaVisita_conOtroQueQueda_laObraPasaAlOtro() = runBlocking {
        almacen.guardar(relevamiento("viejo", fecha = 100L, proxima = 5_000L, hora = 8 * 60))
        almacen.guardar(relevamiento("reciente", fecha = 200L, proxima = 9_000L, hora = 16 * 60))
        assertEquals(9_000L, almacen.obra.proximaVisita)

        borrar("obra-1", "reciente")

        assertEquals(5_000L, almacen.obra.proximaVisita)
        assertEquals(8 * 60, almacen.obra.horaVisita)
    }

    @Test
    fun borrarUnRelevamientoSinProximaVisita_noCambiaLaDeLaObra() = runBlocking {
        almacen.guardar(relevamiento("con", fecha = 100L, proxima = 5_000L, hora = 600))
        almacen.guardar(relevamiento("sin", fecha = 200L, proxima = null))

        borrar("obra-1", "sin")

        assertEquals(5_000L, almacen.obra.proximaVisita)
        assertEquals(600, almacen.obra.horaVisita)
    }

    @Test
    fun siFallaElBorrado_devuelveElError_yNoToCaLaObra() = runBlocking {
        almacen.guardar(relevamiento("r1", fecha = 100L, proxima = 5_000L))
        almacen.errorAlBorrar = RelevamientoError.Red

        val resultado = borrar("obra-1", "r1")

        assertEquals(RelevamientoError.Red, (resultado as AppResult.Error).error)
        assertEquals(0, almacen.setProximaVisitaCalls)
        assertEquals(5_000L, almacen.obra.proximaVisita)
    }

    @Test
    fun siFallaLaLecturaDeLosRelevamientos_informaElError_yNoToCaLaObra() = runBlocking {
        almacen.guardar(relevamiento("r1", fecha = 100L, proxima = 5_000L))
        almacen.errorAlLeer = RelevamientoError.Red

        val resultado = borrar("obra-1", "r1")

        assertEquals(RelevamientoError.Red, (resultado as AppResult.Error).error)
        assertEquals(0, almacen.setProximaVisitaCalls)
    }

    @Test
    fun siFallaLaEscrituraDeLaObra_informaElError() = runBlocking {
        almacen.guardar(relevamiento("r1", fecha = 100L, proxima = 5_000L))
        almacen.errorAlEscribirObra = ObraError.Red

        val resultado = borrar("obra-1", "r1")

        assertEquals(ObraError.Red, (resultado as AppResult.Error).error)
    }

    // ---- Guardar (editar) un relevamiento

    @Test
    fun editarQuitandoLaProximaVisita_laObraQuedaSinProximaVisitaNiHora() = runBlocking {
        almacen.guardar(relevamiento("r1", fecha = 100L, proxima = 5_000L, hora = 600))

        val resultado = actualizar("uid", "obra-1", "r1", RelevamientoDraft(fecha = 100L, nota = "Nota"))

        assertTrue(resultado is AppResult.Success)
        assertNull(almacen.obra.proximaVisita)
        assertNull(almacen.obra.horaVisita)
    }

    @Test
    fun editarQuitandoLaProximaVisita_conOtroQueTiene_laObraUsaLaDelOtro() = runBlocking {
        almacen.guardar(relevamiento("otro", fecha = 50L, proxima = 3_000L, hora = 540))
        almacen.guardar(relevamiento("r1", fecha = 100L, proxima = 5_000L, hora = 600))

        actualizar("uid", "obra-1", "r1", RelevamientoDraft(fecha = 100L, nota = "Nota"))

        assertEquals(3_000L, almacen.obra.proximaVisita)
        assertEquals(540, almacen.obra.horaVisita)
    }

    @Test
    fun editarConProximaVisita_noRecalcula() = runBlocking {
        almacen.guardar(relevamiento("r1", fecha = 100L, proxima = 5_000L))

        actualizar("uid", "obra-1", "r1", RelevamientoDraft(fecha = 100L, nota = "Nota", proximaVisita = 6_000L, horaVisita = 720))

        assertEquals(0, almacen.setProximaVisitaCalls)
        assertEquals(6_000L, almacen.obra.proximaVisita)
        assertEquals(720, almacen.obra.horaVisita)
    }
}
