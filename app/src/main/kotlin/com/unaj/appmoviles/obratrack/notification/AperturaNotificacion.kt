package com.unaj.appmoviles.obratrack.notification

import com.unaj.appmoviles.obratrack.navigation.SessionUiState

/** Acción del intent de una notificación que abre el detalle de una obra. */
const val ACTION_ABRIR_OBRA = "com.unaj.appmoviles.obratrack.action.ABRIR_OBRA"

private const val PREFIJO_URI_OBRA = "obratrack://obra/"
private const val LARGO_MAXIMO_OBRA_ID = 128
private val formatoObraId = Regex("[A-Za-z0-9_-]+")

/**
 * El intent solo lleva el id de la obra (nada de nombre, cliente ni dirección). El dato
 * `obratrack://obra/{obraId}` también distingue el PendingIntent de cada obra.
 */
fun uriObra(obraId: String): String = PREFIJO_URI_OBRA + obraId

/** Ids de Firestore: no vacíos, cortos y solo con [A-Za-z0-9_-]. Lo demás se ignora. */
fun esObraIdValido(obraId: String?): Boolean =
    obraId != null && obraId.length <= LARGO_MAXIMO_OBRA_ID && formatoObraId.matches(obraId)

/**
 * Obra que pide abrir un intent, o null si no es de una notificación de obra.
 * Abrir desde Recientes repite el intent original: se ignora para no volver a navegar.
 */
fun obraIdDeIntent(action: String?, data: String?, lanzadoDesdeHistorial: Boolean): String? {
    if (action != ACTION_ABRIR_OBRA || lanzadoDesdeHistorial) return null
    if (data == null || !data.startsWith(PREFIJO_URI_OBRA)) return null
    return data.removePrefix(PREFIJO_URI_OBRA).takeIf(::esObraIdValido)
}

sealed interface DecisionApertura {
    /** No hay nada pendiente. */
    data object Nada : DecisionApertura
    /** La sesión todavía carga: se decide después. */
    data object Esperar : DecisionApertura
    /** Sin sesión: la apertura sigue pendiente y el login lo avisa. */
    data object PedirLogin : DecisionApertura
    data class AbrirObra(val obraId: String) : DecisionApertura
}

fun decidirApertura(obraIdPendiente: String?, sesion: SessionUiState): DecisionApertura = when {
    obraIdPendiente == null -> DecisionApertura.Nada
    sesion == SessionUiState.Loading -> DecisionApertura.Esperar
    sesion == SessionUiState.Unauthenticated -> DecisionApertura.PedirLogin
    else -> DecisionApertura.AbrirObra(obraIdPendiente)
}
