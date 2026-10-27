package com.unaj.appmoviles.obratrack.core.domain.usecase.obras

import com.unaj.appmoviles.obratrack.core.domain.model.CategoriaAgendaVisita
import com.unaj.appmoviles.obratrack.core.domain.model.EstadoObra
import com.unaj.appmoviles.obratrack.core.domain.model.ItemAgendaVisita
import com.unaj.appmoviles.obratrack.core.domain.model.Obra
import com.unaj.appmoviles.obratrack.core.domain.repository.ObraRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import java.time.Clock
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

class ObserveAgendaVisitasUseCase @Inject constructor(
    private val obraRepository: ObraRepository,
    private val clock: Clock
) {
    /**
     * [actualizar] fuerza a recalcular con la hora actual (por ejemplo cada minuto) sin que cambien
     * las obras: una visita con hora pasa a atrasada al llegar esa hora.
     */
    operator fun invoke(userId: String, actualizar: Flow<Unit> = flowOf(Unit)): Flow<List<ItemAgendaVisita>> {
        return combine(obraRepository.observe(userId), actualizar) { obras, _ ->
            val today = LocalDate.now(clock)

            obras.mapNotNull { obra ->
                buildAgendaItem(obra, today)
            }.sortedWith(
                compareBy(
                    { it.categoria.ordinal },
                    { it.fechaProgramadaMillis ?: Long.MAX_VALUE },
                    // En un mismo día, primero la visita más temprana (sin hora cuenta como la hora por defecto).
                    { it.obra.horaVisita ?: HORA_VISITA_POR_DEFECTO_MINUTOS },
                    { -(it.diasSinVisita ?: 0L) }
                )
            )
        }
    }

    /** Con hora, la visita vence al pasar la hora (hoy mismo); sin hora, al terminar el día. */
    private fun estadoDeLaVisita(fecha: LocalDate, hora: Int?, today: LocalDate): EstadoFechaVisita {
        if (hora == null) return estadoFechaVisita(fecha, today)
        return when (val estado = estadoVisitaConHora(fecha, hora, clock)) {
            EstadoVisitaConHora.Atrasada ->
                EstadoFechaVisita.Atrasada(ChronoUnit.DAYS.between(fecha, today).coerceAtLeast(0L))
            EstadoVisitaConHora.Hoy -> EstadoFechaVisita.Hoy
            is EstadoVisitaConHora.Proxima -> EstadoFechaVisita.Proxima(estado.dias)
        }
    }

    private fun buildAgendaItem(obra: Obra, today: LocalDate): ItemAgendaVisita? {
        val proxima = obra.proximaVisita
        if (proxima != null) {
            val estado = estadoDeLaVisita(proxima.toFechaCalendario(), horaVisitaValida(obra.horaVisita), today)
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
