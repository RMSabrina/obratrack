package com.unaj.appmoviles.obratrack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.unaj.appmoviles.obratrack.navigation.AppRoot
import com.unaj.appmoviles.obratrack.ui.theme.ObraTrackTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as ObraTrackApplication).container

        setContent {
            ObraTrackTheme {
                AppRoot(container)
            }
        }
    }
}