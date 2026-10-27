package com.unaj.appmoviles.features.obras.export

import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Estado de la generación de un PDF; [T] es lo que se comparte (en la app, un `Uri`). */
sealed interface EstadoPdf<out T> {
    data object Inactivo : EstadoPdf<Nothing>
    data object Generando : EstadoPdf<Nothing>
    data class Listo<T>(val resultado: T) : EstadoPdf<T>
    data object Error : EstadoPdf<Nothing>
}

/**
 * Genera un PDF a la vez y expone su estado. Vive en el ViewModel (usa su [scope]), así rotar la
 * pantalla no pierde el trabajo ni lo repite. Solo se usa desde el hilo principal.
 */
class GeneradorPdf<T : Any>(private val scope: CoroutineScope) {

    private val _estado = MutableStateFlow<EstadoPdf<T>>(EstadoPdf.Inactivo)
    val estado: StateFlow<EstadoPdf<T>> = _estado.asStateFlow()

    /** Si ya hay uno en curso no hace nada: dos toques seguidos generan un solo PDF. */
    fun generar(trabajo: suspend () -> T?) {
        if (_estado.value is EstadoPdf.Generando) return
        _estado.value = EstadoPdf.Generando
        scope.launch {
            _estado.value = try {
                trabajo()?.let { EstadoPdf.Listo(it) } ?: EstadoPdf.Error
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "No se pudo generar el PDF", e)
                EstadoPdf.Error
            }
        }
    }

    /** La pantalla ya mostró el resultado (compartió o avisó del error). */
    fun consumido() {
        if (_estado.value !is EstadoPdf.Generando) _estado.value = EstadoPdf.Inactivo
    }

    private companion object {
        const val TAG = "GeneradorPdf"
    }
}
