package com.unaj.appmoviles.obratrack.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.unaj.appmoviles.obratrack.core.domain.ui.LoadingContent
import com.unaj.appmoviles.obratrack.notification.AperturaPendienteViewModel
import com.unaj.appmoviles.obratrack.notification.DecisionApertura
import com.unaj.appmoviles.obratrack.notification.decidirApertura


@Composable
fun AppRoot(aperturaPendiente: AperturaPendienteViewModel) {
    val sessionViewModel: SessionViewModel = hiltViewModel()
    val session by sessionViewModel.uiState.collectAsStateWithLifecycle()
    val obraPendiente by aperturaPendiente.obraPendiente.collectAsStateWithLifecycle()
    val decision = decidirApertura(obraPendiente, session)

    when (session) {
        SessionUiState.Loading -> LoadingContent()
        SessionUiState.Unauthenticated -> AuthNavigation(
            mostrarAvisoObraPendiente = decision is DecisionApertura.PedirLogin
        )
        SessionUiState.Authenticated -> AppShell(
            onLogout = {
                aperturaPendiente.descartar()
                sessionViewModel.logout()
            },
            obraAAbrir = (decision as? DecisionApertura.AbrirObra)?.obraId,
            onObraAbierta = aperturaPendiente::consumir
        )
    }
}