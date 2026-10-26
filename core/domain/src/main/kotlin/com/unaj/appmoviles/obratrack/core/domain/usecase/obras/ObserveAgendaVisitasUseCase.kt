package com.unaj.appmoviles.obratrack.core.domain.usecase.obras

import com.unaj.appmoviles.obratrack.core.domain.model.CategoriaAgendaVisita
import com.unaj.appmoviles.obratrack.core.domain.model.EstadoObra
import com.unaj.appmoviles.obratrack.core.domain.model.ItemAgendaVisita
import com.unaj.appmoviles.obratrack.core.domain.model.Obra
import com.unaj.appmoviles.obratrack.core.domain.repository.ObraRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

class ObserveAgendaVisitasUseCase @Inject constructor(
    private val obraRepository: ObraRepository,
    private val clock: Clock
) {
    operator fun invoke(userId: String): Flow<List<ItemAgendaVisita>> {
        return obraRepository.observe(userId).map { obras ->
            val today = LocalDate.now(clock)

            obras.mapNotNull { obra ->
                buildAgendaItem(obra, today)
            }.sortedWith(
                compareBy(
                    { it.categoria.ordinal },
                    { it.fechaProgramadaMillis ?: Long.MAX_VALUE },
                    { -(it.diasSinVisita ?: 0L) }
                )
            )
        }
    }

    private fun buildAgendaItem(obra: Obra, today: LocalDate): ItemAgendaVisita? {
        val proxima = obra.proximaVisita
        if (proxima != null) {
            val estado = estadoFechaVisita(proxima.toFechaCalendario(), today)
            val (cat, diff) = when (estado) {
                is EstadoFechaVisita.Atrasada -> CategoriaAgendaVisita.URGENTE_VENCIDA to -estado.dias
                EstadoFechaVisita.Hoy -> CategoriaAgendaVisita.HOY to 0L
                is EstadoFechaVisita.Proxima ->
                    (if (estado.dias <= 7L) CategoriaAgendaVisita.ESTA_SEMANA else CategoriaAgendaVisita.PROXIMAS) to estado.dias
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
            val ultimaDate = (obra.ultimaVisita ?: obra.fechaInicio).toFechaCalendario()
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
