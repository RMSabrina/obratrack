package com.unaj.appmoviles.obratrack.core.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.unaj.appmoviles.obratrack.core.domain.repository.ConexionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConexionRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context
) : ConexionRepository {

    private val manager = context.getSystemService(ConnectivityManager::class.java)

    override val enLinea: Flow<Boolean> = callbackFlow {
        fun hayInternet(): Boolean {
            val capacidades = manager.getNetworkCapabilities(manager.activeNetwork)
            return capacidades?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true &&
                capacidades.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        }

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { trySend(hayInternet()) }
            override fun onLost(network: Network) { trySend(hayInternet()) }
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                trySend(hayInternet())
            }
        }
        trySend(hayInternet())
        manager.registerDefaultNetworkCallback(callback)
        awaitClose { manager.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()
}
