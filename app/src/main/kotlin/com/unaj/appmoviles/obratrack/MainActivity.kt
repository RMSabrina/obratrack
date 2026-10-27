package com.unaj.appmoviles.obratrack

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.unaj.appmoviles.obratrack.navigation.AppRoot
import com.unaj.appmoviles.obratrack.notification.AperturaPendienteViewModel
import com.unaj.appmoviles.obratrack.ui.theme.FondoDeVentana
import com.unaj.appmoviles.obratrack.ui.theme.ObraTrackTheme
import com.unaj.appmoviles.obratrack.ui.theme.TemaViewModel
import com.unaj.appmoviles.obratrack.ui.theme.esOscuro
import com.unaj.appmoviles.obratrack.ui.theme.sistemaEnOscuro
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val aperturaPendiente: AperturaPendienteViewModel by viewModels()
    private val tema: TemaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Con el tema ya elegido desde el primer cuadro: las barras del sistema no destellan con el equivocado.
        aplicarBarras(esOscuro(tema.modo.value, sistemaEnOscuro()))
        // Al rotar o restaurar, el intent es el mismo de antes: ya se usó.
        if (savedInstanceState == null) recibirIntent(intent)

        setContent {
            val modo by tema.modo.collectAsStateWithLifecycle()
            val oscuro = esOscuro(modo, sistemaEnOscuro())
            // Íconos de las barras según el tema efectivo, no el del sistema.
            DisposableEffect(oscuro) {
                aplicarBarras(oscuro)
                onDispose { }
            }
            ObraTrackTheme(darkTheme = oscuro) {
                FondoDeVentana()
                AppRoot(aperturaPendiente = aperturaPendiente)
            }
        }
    }

    private fun aplicarBarras(oscuro: Boolean) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { oscuro },
            navigationBarStyle = SystemBarStyle.auto(ESCRIM_CLARO, ESCRIM_OSCURO) { oscuro }
        )
    }

    // App abierta o en segundo plano: la notificación llega acá (SINGLE_TOP).
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        recibirIntent(intent)
    }

    private fun recibirIntent(intent: Intent?) {
        if (intent == null) return
        aperturaPendiente.recibir(
            action = intent.action,
            data = intent.dataString,
            lanzadoDesdeHistorial = intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0
        )
    }

    private companion object {
        // Los de enableEdgeToEdge() por defecto, para la barra de navegación de tres botones.
        val ESCRIM_CLARO = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
        val ESCRIM_OSCURO = Color.argb(0x80, 0x1b, 0x1b, 0x1b)
    }
}
