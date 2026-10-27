package com.unaj.appmoviles.obratrack.navigation

import androidx.navigation3.runtime.NavKey

/**
 * Apila [destino] salvo que la pantalla de arriba sea exactamente la misma. Se compara la
 * pantalla completa, con sus argumentos (los destinos son `data class`): de `ObraDetail(A)` a
 * `ObraDetail(B)` apila, repetir `ObraDetail(A)` (un doble toque) no. Devuelve si apiló.
 */
fun MutableList<NavKey>.navegarA(destino: NavKey): Boolean {
    if (lastOrNull() == destino) return false
    add(destino)
    return true
}
