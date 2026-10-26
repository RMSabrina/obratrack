package com.unaj.appmoviles.obratrack.core.domain.usecase

import com.unaj.appmoviles.obratrack.core.domain.error.AppResult
import com.unaj.appmoviles.obratrack.core.domain.model.CategoriaAgendaVisita
import com.unaj.appmoviles.obratrack.core.domain.model.EstadoObra
import com.unaj.appmoviles.obratrack.core.domain.model.Obra
import com.unaj.appmoviles.obratrack.core.domain.model.ObraDraft
import com.unaj.appmoviles.obratrack.core.domain.repository.ObraRepository
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.ObserveAgendaVisitasUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.ScheduleVisitaUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

class AgendaVisitasUseCasesTest {

    private val baseDate = LocalDate.of(2026, 10, 24)
    // 22:00 del 24/10 en Buenos Aires (01:00 UTC del 25): "hoy" es el 24.
    private val clock = Clock.fixed(Instant.parse("2026-10-25T01:00:00Z"), ZoneId.of("America/Argentina/Buenos_Aires"))

    private fun localDateToUtcMillis(date: LocalDate): Long =
        date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    @Test
    fun observeAgenda_clasificaCorrectamenteVencidasHoyEstaSemanaYProximas() = runBlocking {
        val obraVencida = Obra(
            id = "1", userId = "u1", nombre = "Obra Vencida", cliente = "C1",
            direccion = "D1", publica = false, estado = EstadoObra.ACTIVA,
            fechaInicio = 1000L, ultimaVisita = null,
            proximaVisita = localDateToUtcMillis(baseDate.minusDays(2))
        )
        val obraHoy = Obra(
            id = "2", userId = "u1", nombre = "Obra Hoy", cliente = "C2",
            direccion = "D2", publica = false, estado = EstadoObra.ACTIVA,
            fechaInicio = 1000L, ultimaVisita = null,
            proximaVisita = localDateToUtcMillis(baseDate)
        )
        val obraEstaSemana = Obra(
            id = "3", userId = "u1", nombre = "Obra Esta Semana", cliente = "C3",
            direccion = "D3", publica = false, estado = EstadoObra.ACTIVA,
            fechaInicio = 1000L, ultimaVisita = null,
            proximaVisita = localDateToUtcMillis(baseDate.plusDays(4))
        )
        val obraProxima = Obra(
            id = "4", userId = "u1", nombre = "Obra Lejana", cliente = "C4",
            direccion = "D4", publica = false, estado = EstadoObra.ACTIVA,
            fechaInicio = 1000L, ultimaVisita = null,
            proximaVisita = localDateToUtcMillis(baseDate.plusDays(15))
        )
        val obraSinProgramar = Obra(
            id = "5", userId = "u1", nombre = "Obra Sin Fecha", cliente = "C5",
            direccion = "D5", publica = false, estado = EstadoObra.ACTIVA,
            fechaInicio = localDateToUtcMillis(baseDate.minusDays(10)),
            ultimaVisita = null,
            proximaVisita = null
        )

        val repo = object : FakeBaseObraRepository() {
            override fun observe(userId: String): Flow<List<Obra>> =
                flowOf(listOf(obraProxima, obraSinProgramar, obraVencida, obraEstaSemana, obraHoy))
        }

        val useCase = ObserveAgendaVisitasUseCase(repo, clock)
        val items = useCase("u1").first()

        assertEquals(5, items.size)
        // 1ro: Vencida
        assertEquals(CategoriaAgendaVisita.URGENTE_VENCIDA, items[0].categoria)
        assertEquals("Obra Vencida", items[0].obra.nombre)
        assertEquals(-2L, items[0].diasDiferencia)

        // 2do: Hoy
        assertEquals(CategoriaAgendaVisita.HOY, items[1].categoria)
        assertEquals("Obra Hoy", items[1].obra.nombre)
        assertEquals(0L, items[1].diasDiferencia)

        // 3ro: Esta Semana
        assertEquals(CategoriaAgendaVisita.ESTA_SEMANA, items[2].categoria)
        assertEquals("Obra Esta Semana", items[2].obra.nombre)

        // 4to: Próximas
        assertEquals(CategoriaAgendaVisita.PROXIMAS, items[3].categoria)
        assertEquals("Obra Lejana", items[3].obra.nombre)

        // 5to: Sin Programar
        assertEquals(CategoriaAgendaVisita.SIN_PROGRAMAR, items[4].categoria)
        assertEquals("Obra Sin Fecha", items[4].obra.nombre)
        assertEquals(10L, items[4].diasSinVisita)
    }

    @Test
    fun scheduleVisita_actualizaFechaProximaEnRepositorio() = runBlocking {
        val obra = Obra(
            id = "o1", userId = "u1", nombre = "Torre Norte", cliente = "Cliente A",
            direccion = "Av. Libertador 100", publica = false, estado = EstadoObra.ACTIVA,
            fechaInicio = 1000L, ultimaVisita = null, proximaVisita = null
        )

        var updatedDraft: ObraDraft? = null
        val repo = object : FakeBaseObraRepository() {
            override suspend fun update(userId: String, obraId: String, draft: ObraDraft): AppResult<Obra> {
                updatedDraft = draft
                return AppResult.Success(
                    obra.copy(proximaVisita = draft.proximaVisita)
                )
            }
        }

        val newVisitDate = 1792876800000L
        val useCase = ScheduleVisitaUseCase(repo)
        val result = useCase("u1", obra, newVisitDate)

        assertTrue(result is AppResult.Success)
        assertEquals(newVisitDate, updatedDraft?.proximaVisita)
        assertEquals("Torre Norte", updatedDraft?.nombre)
    }
}

open class FakeBaseObraRepository : ObraRepository {
    override fun observe(userId: String): Flow<List<Obra>> = flowOf(emptyList())
    override fun observeOne(userId: String, obraId: String): Flow<Obra?> = flowOf(null)
    override fun observePublicas(): Flow<List<Obra>> = flowOf(emptyList())
    override fun observePublica(obraId: String): Flow<Obra?> = flowOf(null)
    override suspend fun create(userId: String, draft: ObraDraft): AppResult<Obra> =
        AppResult.Error(com.unaj.appmoviles.obratrack.core.domain.error.ObraError.NombreVacio)
    override suspend fun update(userId: String, obraId: String, draft: ObraDraft): AppResult<Obra> =
        AppResult.Error(com.unaj.appmoviles.obratrack.core.domain.error.ObraError.NombreVacio)
    override suspend fun existsAndOwnedByCurrentUser(obraId: String): AppResult<Boolean> =
        AppResult.Success(true)
    override suspend fun delete(obraId: String): AppResult<Unit> = AppResult.Success(Unit)
    override suspend fun deleteAllSuscriptoresByObra(obraId: String): AppResult<Unit> = AppResult.Success(Unit)
}
