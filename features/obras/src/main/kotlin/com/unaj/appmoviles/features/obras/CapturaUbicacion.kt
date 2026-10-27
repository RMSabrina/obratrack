package com.unaj.appmoviles.features.obras

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.unaj.appmoviles.obratrack.core.domain.model.Ubicacion

private const val TAG = "CapturaUbicacion"

/**
 * Captura de la ubicación del GPS de un formulario. [lanzar] pide la ubicación y devuelve cómo
 * cancelarla; [alTerminar] recibe el resultado (null = no se pudo). Un pedido cancelado a mano no
 * llama a [alTerminar]. Rotar la pantalla corta el pedido pero conserva [capturando]: la
 * pantalla nueva lo retoma con [retomarSiCorresponde] en lugar de perderlo sin avisar.
 */
class ControlCaptura(
    capturandoInicial: Boolean,
    private val lanzar: (onResult: (Ubicacion?) -> Unit) -> () -> Unit,
    private val alTerminar: (Ubicacion?) -> Unit
) {
    var capturando by mutableStateOf(capturandoInicial)
        private set

    private var cancelarActual: (() -> Unit)? = null
    private var generacion = 0

    fun iniciar() {
        if (cancelarActual != null) return
        capturando = true
        val mia = ++generacion
        val cancelar = lanzar { ubicacion ->
            if (mia == generacion) {
                cancelarActual = null
                capturando = false
                alTerminar(ubicacion)
            }
        }
        // Si el resultado llegó antes de volver de lanzar(), ya no hay nada que cancelar.
        if (mia == generacion && capturando) cancelarActual = cancelar
    }

    /** El usuario cancela: no se avisa ningún resultado. */
    fun cancelar() {
        generacion++
        cancelarActual?.invoke()
        cancelarActual = null
        capturando = false
    }

    /** La pantalla se destruye: corta el pedido pero deja [capturando] para retomarlo. */
    fun liberar() {
        generacion++
        cancelarActual?.invoke()
        cancelarActual = null
    }

    fun retomarSiCorresponde() {
        if (capturando && cancelarActual == null) iniciar()
    }
}

@Composable
fun rememberCapturaUbicacion(onResultado: (Ubicacion?) -> Unit): ControlCaptura {
    val contexto by rememberUpdatedState(LocalContext.current)
    val alTerminar by rememberUpdatedState(onResultado)
    fun crear(capturando: Boolean) = ControlCaptura(
        capturandoInicial = capturando,
        lanzar = { onResult -> capturarUbicacion(contexto, onResult) },
        alTerminar = { alTerminar(it) }
    )
    val control = rememberSaveable(
        saver = Saver<ControlCaptura, Boolean>(save = { it.capturando }, restore = { crear(it) })
    ) { crear(false) }
    DisposableEffect(control) { onDispose { control.liberar() } }
    LaunchedEffect(control) { control.retomarSiCorresponde() }
    return control
}

/** Pide la ubicación al GPS (la última conocida si es reciente) y devuelve cómo cancelar el pedido. */
private fun capturarUbicacion(context: Context, onResult: (Ubicacion?) -> Unit): () -> Unit {
    val cancellationTokenSource = CancellationTokenSource()
    try {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        fusedLocationClient.lastLocation.addOnSuccessListener { lastLocation ->
            if (lastLocation != null && (System.currentTimeMillis() - lastLocation.time) < 120_000) {
                onResult(Ubicacion(lastLocation.latitude, lastLocation.longitude))
                return@addOnSuccessListener
            }
            // Cancelado mientras se pedía la última ubicación: no se pide una nueva.
            if (cancellationTokenSource.token.isCancellationRequested) return@addOnSuccessListener

            val request = CurrentLocationRequest.Builder()
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setMaxUpdateAgeMillis(60_000)
                .setDurationMillis(15_000)
                .build()

            fusedLocationClient.getCurrentLocation(request, cancellationTokenSource.token)
                .addOnSuccessListener { location ->
                    onResult(location?.let { Ubicacion(it.latitude, it.longitude) })
                }
                .addOnFailureListener {
                    onResult(null)
                }
                .addOnCanceledListener {
                    onResult(null)
                }
        }.addOnFailureListener {
            onResult(null)
        }
    } catch (e: SecurityException) {
        // El permiso se quitó entre pedirlo y usarlo.
        Log.w(TAG, "Sin permiso de ubicación", e)
        onResult(null)
    }
    return { cancellationTokenSource.cancel() }
}
