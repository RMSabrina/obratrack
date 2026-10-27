package com.unaj.appmoviles.obratrack.ui.theme

import android.app.Activity
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.drawable.ColorDrawable
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import com.unaj.appmoviles.obratrack.core.domain.model.ModoTema

/** Si el tema que se dibuja es el oscuro: el elegido en la app o, en "Sistema", el del dispositivo. */
fun esOscuro(modo: ModoTema, sistemaOscuro: Boolean): Boolean = when (modo) {
    ModoTema.SISTEMA -> sistemaOscuro
    ModoTema.CLARO -> false
    ModoTema.OSCURO -> true
}

/** Modo de AppCompat para los recursos XML (`values-night`); solo se aplica al arrancar el proceso. */
fun ModoTema.aNightMode(): Int = when (this) {
    ModoTema.SISTEMA -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    ModoTema.CLARO -> AppCompatDelegate.MODE_NIGHT_NO
    ModoTema.OSCURO -> AppCompatDelegate.MODE_NIGHT_YES
}

/**
 * Tema del dispositivo. Se lee de los recursos del sistema y no de `isSystemInDarkTheme()`: con
 * `setDefaultNightMode` forzado a claro u oscuro, ese valor ya no es el del sistema.
 */
fun sistemaEnOscuro(): Boolean =
    (Resources.getSystem().configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
        Configuration.UI_MODE_NIGHT_YES

/** Pinta el fondo de la ventana con el del tema: al cambiar de tema en vivo no queda el del recurso XML. */
@Composable
fun FondoDeVentana() {
    val vista = LocalView.current
    val fondo = MaterialTheme.colorScheme.background
    SideEffect {
        (vista.context as? Activity)?.window?.setBackgroundDrawable(ColorDrawable(fondo.toArgb()))
    }
}
