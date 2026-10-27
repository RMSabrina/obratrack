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

/** Pantallas con barra inferior: Inicio, Obras, Agenda y Perfil. El resto (detalles, formularios) no la muestra. */
fun NavKey.esDeNivelSuperior(): Boolean =
    this is HomeDestination || this is ObrasDestination || this is AgendaDestination || this is ProfileDestination

/**
 * Cambia a la pestaña [destino] dejando la pila en `[Home]` o `[Home, destino]`: lo que había encima se
 * descarta, así "atrás" desde una pestaña vuelve a Inicio. Si [destino] ya está arriba no hace nada
 * (un doble toque no apila). Devuelve si cambió la pila.
 */
fun MutableList<NavKey>.cambiarDePestana(destino: NavKey): Boolean {
    if (lastOrNull() == destino) return false
    if (firstOrNull() != HomeDestination) {
        clear()
        add(HomeDestination)
    }
    while (size > 1) removeAt(lastIndex)
    if (destino != HomeDestination) add(destino)
    return true
}
