package com.unaj.appmoviles.features.obras.tareas.cronograma

import com.unaj.appmoviles.obratrack.core.domain.error.DomainError

/** [cronograma] es null mientras carga o si hubo error; "hoy" se tomó del Clock en el ViewModel. */
data class CronogramaUiState(
    val isLoading: Boolean = true,
    val cronograma: Cronograma? = null,
    val error: DomainError? = null
)
