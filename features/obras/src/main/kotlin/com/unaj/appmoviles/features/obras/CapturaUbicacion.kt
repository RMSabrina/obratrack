package com.unaj.appmoviles.features.obras

import android.app.Activity
import android.content.Context
import android.content.IntentSender
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
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
    // Resultado del diálogo "activar ubicación" que está abierto, si hay uno.
    var respuestaDialogo by remember { mutableStateOf<((Boolean) -> Unit)?>(null) }
    val dialogoAjustes = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { resultado ->
        val responder = respuestaDialogo
        respuestaDialogo = null
        responder?.invoke(resultado.resultCode == Activity.RESULT_OK)
    }
    fun crear(capturando: Boolean) = ControlCaptura(
        capturandoInicial = capturando,
        lanzar = { onResult ->
            val token = CancellationTokenSource()
            comprobarAjustesYCapturar(
                contexto = contexto,
                token = token,
                pedirActivacion = { intentSender, alResponder ->
                    respuestaDialogo = alResponder
                    dialogoAjustes.launch(IntentSenderRequest.Builder(intentSender).build())
                },
                onResult = onResult
            )
            return@ControlCaptura {
                respuestaDialogo = null
                token.cancel()
            }
        },
        alTerminar = { alTerminar(it) }
    )
    val control = rememberSaveable(
        saver = Saver<ControlCaptura, Boolean>(save = { it.capturando }, restore = { crear(it) })
    ) { crear(false) }
    DisposableEffect(control) { onDispose { control.liberar() } }
    LaunchedEffect(control) { control.retomarSiCorresponde() }
    return control
}

/**
 * Con la ubicación del teléfono apagada, pide activarla con el diálogo del sistema (si el usuario
 * acepta se captura; si no, [onResult] recibe null). Si no se puede resolver, se intenta igual.
 */
private fun comprobarAjustesYCapturar(
    contexto: Context,
    token: CancellationTokenSource,
    pedirActivacion: (IntentSender, (Boolean) -> Unit) -> Unit,
    onResult: (Ubicacion?) -> Unit
) {
    val ajustes = LocationSettingsRequest.Builder()
        .addLocationRequest(LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10_000).build())
        .build()
    LocationServices.getSettingsClient(contexto).checkLocationSettings(ajustes)
        .addOnSuccessListener {
            if (!token.token.isCancellationRequested) capturarUbicacion(contexto, token, onResult)
        }
        .addOnFailureListener { e ->
            when {
                token.token.isCancellationRequested -> Unit
                e is ResolvableApiException -> try {
                    pedirActivacion(e.resolution.intentSender) { activada ->
                        if (activada) capturarUbicacion(contexto, token, onResult) else onResult(null)
                    }
                } catch (ex: IntentSender.SendIntentException) {
                    Log.w(TAG, "No se pudo mostrar el diálogo de ubicación", ex)
                    onResult(null)
                }
                else -> capturarUbicacion(contexto, token, onResult)
            }
        }
}

/** Pide la ubicación al GPS (la última conocida si es reciente); [token] permite cancelar el pedido. */
private fun capturarUbicacion(context: Context, cancellationTokenSource: CancellationTokenSource, onResult: (Ubicacion?) -> Unit) {
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
}
