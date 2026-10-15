package com.unaj.appmoviles.obratrack

import AppContainer
import android.app.Application

class ObraTrackApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer()
    }
}