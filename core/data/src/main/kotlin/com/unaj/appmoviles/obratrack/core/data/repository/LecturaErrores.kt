package com.unaj.appmoviles.obratrack.core.data.repository

import com.unaj.appmoviles.obratrack.core.domain.error.DomainError
import com.unaj.appmoviles.obratrack.core.domain.error.LecturaFallidaException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch

/**
 * Convierte la falla del Flow en una [LecturaFallidaException] con el error de dominio, para
 * que los ViewModels no vean excepciones de Firebase. `catch` no atrapa la cancelación.
 */
internal fun <T> Flow<T>.conErrorDeLectura(clasificar: (Exception) -> DomainError): Flow<T> =
    catch { e ->
        throw LecturaFallidaException(
            error = if (e is Exception) clasificar(e) else clasificar(RuntimeException(e)),
            causa = e
        )
    }
