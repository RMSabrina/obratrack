package com.unaj.appmoviles.obratrack.core.domain.usecase.obras

import com.unaj.appmoviles.obratrack.core.domain.model.CategoriaAgendaVisita
import com.unaj.appmoviles.obratrack.core.domain.model.EstadoObra
import com.unaj.appmoviles.obratrack.core.domain.model.ItemAgendaVisita
import com.unaj.appmoviles.obratrack.core.domain.model.Obra
import com.unaj.appmoviles.obratrack.core.domain.repository.ObraRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import javax.inject.Inject

class ObserveAgendaVisitasUseCase @Inject constructor(
    private val obraRepository: ObraRepository
) {
    operator fun invoke(
        userId: String,
        zoneId: ZoneId = ZoneId.systemDefault(),
        customToday: LocalDate? = null
    ): Flow<List<ItemAgendaVisita>> {
        return obraRepository.observe(userId).map { obras ->
            val today = customToday ?: LocalDate.now(zoneId)
            
            obras.mapNotNull { obra ->
                buildAgendaItem(obra, today, zoneId)
            }.sortedWith(
                compareBy(
                    { it.categoria.ordinal },
                    { it.fechaProgramadaMillis ?: Long.MAX_VALUE },
                    { -(it.diasSinVisita ?: 0L) }
                )
            )
        }
    }

    private fun buildAgendaItem(obra: Obra, today: LocalDate, zoneId: ZoneId): ItemAgendaVisita? {
        val proxima = obra.proximaVisita
        if (proxima != null) {
            // Evaluamos la fecha en UTC (formato nativo de DatePicker) o en zoneId si es hora local
            val visitDate = try {
                Instant.ofEpochMilli(proxima).atZone(ZoneOffset.UTC).toLocalDate()
            } catch (_: Exception) {
                Instant.ofEpochMilli(proxima).atZone(zoneId).toLocalDate()
            }
            val diff = ChronoUnit.DAYS.between(today, visitDate)
            val cat = when {
                diff < 0 -> CategoriaAgendaVisita.URGENTE_VENCIDA
                diff == 0L -> CategoriaAgendaVisita.HOY
                diff in 1L..7L -> CategoriaAgendaVisita.ESTA_SEMANA
                else -> CategoriaAgendaVisita.PROXIMAS
            }
            return ItemAgendaVisita(
                obra = obra,
                fechaProgramadaMillis = proxima,
                categoria = cat,
                diasDiferencia = diff,
                ultimaVisitaMillis = obra.ultimaVisita
            )
        }

        // Si no tiene fecha programada pero la obra está activa, alertamos para agendar visita
        if (obra.estado == EstadoObra.ACTIVA) {
            val ultima = obra.ultimaVisita ?: obra.fechaInicio
            val ultimaDate = try {
                Instant.ofEpochMilli(ultima).atZone(ZoneOffset.UTC).toLocalDate()
            } catch (_: Exception) {
                Instant.ofEpochMilli(ultima).atZone(zoneId).toLocalDate()
            }
            val sinVisita = ChronoUnit.DAYS.between(ultimaDate, today).coerceAtLeast(0L)
            return ItemAgendaVisita(
                obra = obra,
                fechaProgramadaMillis = null,
                categoria = CategoriaAgendaVisita.SIN_PROGRAMAR,
                diasDiferencia = 0L,
                ultimaVisitaMillis = obra.ultimaVisita,
                diasSinVisita = sinVisita
            )
        }

        return null
    }
}
