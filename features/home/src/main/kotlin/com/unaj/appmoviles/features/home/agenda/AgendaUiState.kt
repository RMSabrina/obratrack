package com.unaj.appmoviles.features.home.agenda

import com.unaj.appmoviles.obratrack.core.domain.error.DomainError
import com.unaj.appmoviles.obratrack.core.domain.model.CategoriaAgendaVisita
import com.unaj.appmoviles.obratrack.core.domain.model.ItemAgendaVisita
import com.unaj.appmoviles.obratrack.core.domain.model.Obra

enum class FiltroAgenda(val label: String) {
    TODAS("Todas"),
    URGENTES_Y_HOY("🚨 Urgentes y Hoy"),
    ESTA_SEMANA("📅 Esta semana"),
    PROXIMAS("⏳ Próximas"),
    SIN_PROGRAMAR("⚠️ Sin programar")
}

data class AgendaUiState(
    val isLoading: Boolean = true,
    val items: List<ItemAgendaVisita> = emptyList(),
    val filtro: FiltroAgenda = FiltroAgenda.TODAS,
    val obraParaReprogramar: Obra? = null,
    val isUpdating: Boolean = false,
    val error: DomainError? = null,
    val mensajeFeedback: String? = null
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
