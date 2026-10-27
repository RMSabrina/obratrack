package com.unaj.appmoviles.features.obras

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.unaj.appmoviles.obratrack.core.ui.components.DescartarCambiosDialog

/**
 * Devuelve la acción de salir de un formulario: con [hayCambios] pregunta antes de descartar,
 * y el botón Atrás del sistema pasa por la misma pregunta. Usarla también en la flecha de la barra.
 */
@Composable
fun rememberSalidaConConfirmacion(hayCambios: Boolean, onSalir: () -> Unit): () -> Unit {
    var confirmando by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = hayCambios) { confirmando = true }
    if (confirmando) {
        DescartarCambiosDialog(
            onDescartar = {
                confirmando = false
                onSalir()
            },
            onSeguir = { confirmando = false }
        )
    }
    return { if (hayCambios) confirmando = true else onSalir() }
}
