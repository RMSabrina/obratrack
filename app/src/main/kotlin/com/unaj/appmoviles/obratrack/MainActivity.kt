package com.unaj.appmoviles.obratrack

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.unaj.appmoviles.obratrack.navigation.AppRoot
import com.unaj.appmoviles.obratrack.notification.AperturaPendienteViewModel
import com.unaj.appmoviles.obratrack.ui.theme.ObraTrackTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val aperturaPendiente: AperturaPendienteViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Al rotar o restaurar, el intent es el mismo de antes: ya se usó.
        if (savedInstanceState == null) recibirIntent(intent)

        setContent {
            ObraTrackTheme {
                AppRoot(aperturaPendiente = aperturaPendiente)
            }
        }
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
}