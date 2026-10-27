package com.unaj.appmoviles.obratrack.core.domain.usecase

import com.unaj.appmoviles.obratrack.core.domain.error.AppResult
import com.unaj.appmoviles.obratrack.core.domain.model.CategoriaAgendaVisita
import com.unaj.appmoviles.obratrack.core.domain.model.EstadoObra
import com.unaj.appmoviles.obratrack.core.domain.model.Obra
import com.unaj.appmoviles.obratrack.core.domain.model.ObraDraft
import com.unaj.appmoviles.obratrack.core.domain.repository.ObraRepository
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.ObserveAgendaVisitasUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.ScheduleVisitaUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
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
        val result = useCase("u1", obra, newVisitDate, null)

        assertTrue(result is AppResult.Success)
        assertEquals(newVisitDate, updatedDraft?.proximaVisita)
        assertEquals("Torre Norte", updatedDraft?.nombre)
    }

    @Test
    fun observeAgenda_mismoDia_ordenaPorHora() = runBlocking {
        fun obra(id: String, hora: Int?) = Obra(
            id = id, userId = "u1", nombre = id, cliente = "C", direccion = "D", publica = false,
            estado = EstadoObra.ACTIVA, fechaInicio = 1000L, ultimaVisita = null,
            proximaVisita = localDateToUtcMillis(baseDate.plusDays(3)), horaVisita = hora
        )
        val repo = object : FakeBaseObraRepository() {
            override fun observe(userId: String): Flow<List<Obra>> =
                flowOf(listOf(obra("tarde", 15 * 60), obra("temprano", 8 * 60), obra("sinHora", null)))
        }

        val items = ObserveAgendaVisitasUseCase(repo, clock)("u1").first()

        assertEquals(listOf("temprano", "sinHora", "tarde"), items.map { it.obra.id })
    }

    private fun categoriaDe(hora: Int?): Pair<CategoriaAgendaVisita, Long> = runBlocking {
        val obra = obraConVisita(localDateToUtcMillis(baseDate), hora)
        val repo = object : FakeBaseObraRepository() {
            override fun observe(userId: String): Flow<List<Obra>> = flowOf(listOf(obra))
        }
        // El reloj marca las 22:00 del 24/10 en Buenos Aires.
        val item = ObserveAgendaVisitasUseCase(repo, clock)("u1").first().single()
        item.categoria to item.diasDiferencia
    }

    @Test
    fun observeAgenda_visitaDeHoyConHoraPasada_estaAtrasadaYNoEsCeroDias() {
        assertEquals(CategoriaAgendaVisita.URGENTE_VENCIDA to 0L, categoriaDe(20 * 60 + 30))
    }

    @Test
    fun observeAgenda_visitaDeHoyConHoraFutura_siguePendienteHoy() {
        assertEquals(CategoriaAgendaVisita.HOY to 0L, categoriaDe(23 * 60))
    }

    @Test
    fun observeAgenda_visitaDeHoySinHora_siguePendienteHastaQueTerminaElDia() {
        assertEquals(CategoriaAgendaVisita.HOY to 0L, categoriaDe(null))
    }

    private fun obraConVisita(proxima: Long?, hora: Int?) = Obra(
        id = "o1", userId = "u1", nombre = "Torre", cliente = "C", direccion = "D", publica = false,
        estado = EstadoObra.ACTIVA, fechaInicio = 1000L, ultimaVisita = null, proximaVisita = proxima, horaVisita = hora
    )

    private fun borradorAlProgramar(obra: Obra, nueva: Long?, hora: Int? = obra.horaVisita): ObraDraft? = runBlocking {
        var draft: ObraDraft? = null
        val repo = object : FakeBaseObraRepository() {
            override suspend fun update(userId: String, obraId: String, draft0: ObraDraft): AppResult<Obra> {
                draft = draft0
                return AppResult.Success(obra)
            }
        }
        ScheduleVisitaUseCase(repo)("u1", obra, nueva, hora)
        draft
    }

    @Test
    fun scheduleVisita_conLaMismaFecha_conservaLaHora() {
        val draft = borradorAlProgramar(obraConVisita(5_000L, 600), 5_000L)

        assertEquals(600, draft?.horaVisita)
    }

    @Test
    fun scheduleVisita_conOtraFecha_conservaLaHoraSiSePide() {
        val draft = borradorAlProgramar(obraConVisita(5_000L, 600), 9_000L)

        assertEquals(9_000L, draft?.proximaVisita)
        assertEquals(600, draft?.horaVisita)
    }

    @Test
    fun scheduleVisita_permiteElegirOtraHoraOQuitarla() {
        val obra = obraConVisita(5_000L, 600)

        assertEquals(900, borradorAlProgramar(obra, 9_000L, hora = 900)?.horaVisita)
        assertEquals(null, borradorAlProgramar(obra, 9_000L, hora = null)?.horaVisita)
    }

    @Test
    fun scheduleVisita_conHoraFueraDeRango_laDescarta() {
        assertEquals(null, borradorAlProgramar(obraConVisita(5_000L, 600), 9_000L, hora = 5_000)?.horaVisita)
    }

    @Test
    fun scheduleVisita_quitandoLaFecha_dejaSinHoraAunqueSePida() {
        val draft = borradorAlProgramar(obraConVisita(5_000L, 600), null, hora = 600)

        assertEquals(null, draft?.proximaVisita)
        assertEquals(null, draft?.horaVisita)
    }

    private class RelojMutable(var ahora: Instant, private val zona: ZoneId) : Clock() {
        override fun getZone(): ZoneId = zona
        override fun withZone(zone: ZoneId): Clock = RelojMutable(ahora, zone)
        override fun instant(): Instant = ahora
    }

    @Test
    fun observeAgenda_alLlegarLaHora_laVisitaPasaAAtrasadaSinCambiarLasObras() {
        // 20:00 en Buenos Aires; la visita de hoy es a las 20:30.
        val reloj = RelojMutable(Instant.parse("2026-10-24T23:00:00Z"), ZoneId.of("America/Argentina/Buenos_Aires"))
        val obra = obraConVisita(localDateToUtcMillis(baseDate), 20 * 60 + 30)
        val repo = object : FakeBaseObraRepository() {
            override fun observe(userId: String): Flow<List<Obra>> = flowOf(listOf(obra))
        }
        val tic = MutableStateFlow(0)
        val categorias = mutableListOf<CategoriaAgendaVisita>()
        val job = CoroutineScope(Dispatchers.Unconfined).launch {
            ObserveAgendaVisitasUseCase(repo, reloj)("u1", tic.map { }).collect { categorias += it.single().categoria }
        }

        reloj.ahora = Instant.parse("2026-10-24T23:45:00Z")
        tic.value = 1
        job.cancel()

        assertEquals(listOf(CategoriaAgendaVisita.HOY, CategoriaAgendaVisita.URGENTE_VENCIDA), categorias)
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
