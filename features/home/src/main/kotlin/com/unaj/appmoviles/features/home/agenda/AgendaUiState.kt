package com.unaj.appmoviles.features.home.agenda

import androidx.annotation.StringRes
import com.unaj.appmoviles.home.R
import com.unaj.appmoviles.obratrack.core.domain.error.AppResult
import com.unaj.appmoviles.obratrack.core.domain.error.DomainError
import com.unaj.appmoviles.obratrack.core.domain.model.CategoriaAgendaVisita
import com.unaj.appmoviles.obratrack.core.domain.model.ItemAgendaVisita
import com.unaj.appmoviles.obratrack.core.domain.model.Obra

enum class FiltroAgenda(@StringRes val labelRes: Int) {
    TODAS(R.string.agenda_filtro_todas),
    URGENTES_Y_HOY(R.string.agenda_filtro_urgentes_y_hoy),
    ESTA_SEMANA(R.string.agenda_filtro_esta_semana),
    PROXIMAS(R.string.agenda_filtro_proximas),
    SIN_PROGRAMAR(R.string.agenda_filtro_sin_programar)
}

/** Aviso tras reprogramar una visita; la pantalla arma el texto en el idioma actual. */
sealed interface FeedbackAgenda {
    data class VisitaProgramada(val obraNombre: String) : FeedbackAgenda
    data class VisitaCancelada(val obraNombre: String) : FeedbackAgenda
    data class ErrorAlProgramar(val error: DomainError) : FeedbackAgenda
}

/** Sin fecha nueva (`nuevoMillis == null`) la visita se canceló. */
fun feedbackDeReprogramacion(
    obraNombre: String,
    nuevoMillis: Long?,
    resultado: AppResult<*>
): FeedbackAgenda = when (resultado) {
    is AppResult.Success ->
        if (nuevoMillis != null) {
            FeedbackAgenda.VisitaProgramada(obraNombre)
        } else {
            FeedbackAgenda.VisitaCancelada(obraNombre)
        }
    is AppResult.Error -> FeedbackAgenda.ErrorAlProgramar(resultado.error)
}

data class AgendaUiState(
    val isLoading: Boolean = true,
    val items: List<ItemAgendaVisita> = emptyList(),
    val filtro: FiltroAgenda = FiltroAgenda.TODAS,
    val obraParaReprogramar: Obra? = null,
    val isUpdating: Boolean = false,
    val error: DomainError? = null,
    val mensajeFeedback: FeedbackAgenda? = null,
    /** Hoy como fecha de calendario (medianoche UTC): la fecha inicial del selector de la visita. */
    val hoyMillis: Long = 0L
) {
    val itemsFiltrados: List<ItemAgendaVisita>
        get() = when (filtro) {
            FiltroAgenda.TODAS -> items
            FiltroAgenda.URGENTES_Y_HOY -> items.filter {
                it.categoria == CategoriaAgendaVisita.URGENTE_VENCIDA || it.categoria == CategoriaAgendaVisita.HOY
            }
            FiltroAgenda.ESTA_SEMANA -> items.filter { it.categoria == CategoriaAgendaVisita.ESTA_SEMANA }
            FiltroAgenda.PROXIMAS -> items.filter { it.categoria == CategoriaAgendaVisita.PROXIMAS }
            FiltroAgenda.SIN_PROGRAMAR -> items.filter { it.categoria == CategoriaAgendaVisita.SIN_PROGRAMAR }
        }

    val totalHoy: Int get() = items.count { it.categoria == CategoriaAgendaVisita.HOY }
    val totalVencidas: Int get() = items.count { it.categoria == CategoriaAgendaVisita.URGENTE_VENCIDA }
    val totalSinProgramar: Int get() = items.count { it.categoria == CategoriaAgendaVisita.SIN_PROGRAMAR }
    val totalEstaSemana: Int get() = items.count { it.categoria == CategoriaAgendaVisita.ESTA_SEMANA }
}
