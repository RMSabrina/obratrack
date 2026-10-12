package com.unaj.appmoviles.obratrack.navigation

import AppContainer
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.unaj.appmoviles.features.obras.form.ObraFormInitial
import com.unaj.appmoviles.features.obras.form.ObraFormRoute
import com.unaj.appmoviles.features.obras.list.ObrasListRoute
import com.unaj.appmoviles.features.obras.toStorageValue

@Composable
fun AppShell(container: AppContainer, onLogout: () -> Unit) {
    val backStack = rememberNavBackStack(ObrasDestination)
    fun goBack() {
        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
    }

    NavDisplay(
        backStack = backStack,
        onBack = ::goBack,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        entryProvider = entryProvider {
            entry<ObrasDestination> {
                ObrasListRoute(
                    viewModel = viewModel(factory = container.obrasListViewModelFactory),
                    onCreate = { backStack.add(ObraFormDestination()) },
                    onEdit = { obra ->
                        backStack.add(
                            ObraFormDestination(
                                obraId = obra.id,
                                nombre = obra.nombre,
                                cliente = obra.cliente,
                                direccion = obra.direccion,
                                estado = obra.estado.toStorageValue(),
                                publica = obra.publica
                            )
                        )
                    },
                    onLogout = onLogout
                )
            }
            entry<ObraFormDestination> { destination ->
                ObraFormRoute(
                    viewModel = viewModel(factory = container.obraFormViewModelFactory(destination.obraId)),
                    initial = ObraFormInitial(
                        obraId = destination.obraId,
                        nombre = destination.nombre,
                        cliente = destination.cliente,
                        direccion = destination.direccion,
                        estado = destination.estado,
                        publica = destination.publica
                    ),
                    onBack = ::goBack,
                    onSaved = ::goBack
                )
            }
        }
    )
}
