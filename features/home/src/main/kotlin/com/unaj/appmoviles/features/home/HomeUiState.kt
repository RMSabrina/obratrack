package com.unaj.appmoviles.features.home

import com.unaj.appmoviles.obratrack.core.domain.error.DomainError
import com.unaj.appmoviles.obratrack.core.domain.model.ItemAgendaVisita
import com.unaj.appmoviles.obratrack.core.domain.model.Obra
import com.unaj.appmoviles.obratrack.core.domain.model.Relevamiento

data class HomeUiState(
    val isLoading: Boolean = true,
    /** Vacío si el usuario no tiene nombre: la pantalla muestra el texto por defecto. */
    val userName: String = "",
    val obras: List<Obra> = emptyList(),
    val recientes: List<RelevamientoReciente> = emptyList(),
    val proximaVisitaDestacada: ItemAgendaVisita? = null,
    val totalVisitasHoy: Int = 0,
    val totalVisitasAtrasadas: Int = 0,
    val error: DomainError? = null
)

data class RelevamientoReciente(
    val relevamiento: Relevamiento,
    /** null si la obra ya no está entre las del usuario: la pantalla muestra "Obra desconocida". */
    val obraNombre: String?
)

/** Los 3 relevamientos más recientes, con el nombre de su obra. */
internal fun relevamientosRecientes(
    relevamientos: List<Relevamiento>,
    obras: List<Obra>
): List<RelevamientoReciente> =
    relevamientos
        .sortedByDescending { it.fecha }
        .take(3)
        .map { rel ->
            RelevamientoReciente(
                relevamiento = rel,
                obraNombre = obras.find { it.id == rel.obraId }?.nombre
            )
        }
