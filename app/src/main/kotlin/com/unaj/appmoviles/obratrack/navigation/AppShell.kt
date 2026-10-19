package com.unaj.appmoviles.obratrack.navigation

import AppContainer
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.unaj.appmoviles.features.auth.profile.ProfileRoute
import com.unaj.appmoviles.features.obras.detail.ObraDetailRoute
import com.unaj.appmoviles.features.obras.form.ObraFormInitial
import com.unaj.appmoviles.features.obras.form.ObraFormRoute
import com.unaj.appmoviles.features.obras.list.ObrasListRoute
import com.unaj.appmoviles.features.obras.list.ObrasPublicasListRoute
import com.unaj.appmoviles.features.obras.detail.ObraPublicaDetailRoute
import com.unaj.appmoviles.features.obras.relevamiento.RelevamientoFormInitial
import com.unaj.appmoviles.features.obras.relevamiento.RelevamientoFormRoute
import com.unaj.appmoviles.features.obras.relevamiento.detail.RelevamientoDetailRoute
import com.unaj.appmoviles.features.home.HomeRoute
import com.unaj.appmoviles.features.home.ObraSelectionRoute
import com.unaj.appmoviles.obratrack.core.domain.model.Relevamiento
import com.unaj.appmoviles.features.obras.toStorageValue

@Composable
fun AppShell(container: AppContainer, onLogout: () -> Unit) {
    val backStack = rememberNavBackStack(HomeDestination)
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
            entry<HomeDestination> {
                HomeRoute(
                    viewModel = viewModel(factory = container.homeViewModelFactory),
                    onNavigateToObras = { backStack.add(ObrasDestination) },
                    onNavigateToObraSelection = { backStack.add(ObraSelectionDestination) },
                    onNavigateToObrasPublicas = { backStack.add(ObrasPublicasDestination) },
                    onNavigateToProfile = { backStack.add(ProfileDestination) },
                    onOpenRelevamiento = { relevamiento -> 
                        backStack.add(
                            RelevamientoDetailDestination(
                                obraId = relevamiento.obraId, 
                                relevamientoId = relevamiento.id
                            )
                        ) 
                    }
                )
            }
            entry<ObraSelectionDestination> {
                ObraSelectionRoute(
                    viewModel = viewModel(factory = container.obrasListViewModelFactory),
                    onObraSelected = { obra -> 
                        backStack.removeAt(backStack.lastIndex)
                        backStack.add(RelevamientoFormDestination(obraId = obra.id))
                    },
                    onBack = ::goBack
                )
            }
            entry<ProfileDestination> {
                ProfileRoute(
                    viewModel = viewModel(factory = container.profileViewModelFactory),
                    onBack = ::goBack,
                    onLogout = onLogout
                )
            }
            entry<ObrasDestination> {
                ObrasListRoute(
                    viewModel = viewModel(factory = container.obrasListViewModelFactory),
                    onCreate = { backStack.add(ObraFormDestination()) },
                    onOpen = { obra -> backStack.add(ObraDetailDestination(obra.id)) },
                    onLogout = onLogout
                )
            }
            entry<ObrasPublicasDestination> {
                ObrasPublicasListRoute(
                    viewModel = viewModel(factory = container.obrasPublicasListViewModelFactory),
                    onOpen = { obra -> backStack.add(ObraPublicaDetailDestination(obra.id)) },
                    onBack = ::goBack
                )
            }
            entry<ObraPublicaDetailDestination> { destination ->
                ObraPublicaDetailRoute(
                    viewModel = viewModel(factory = container.obraPublicaDetailViewModelFactory(destination.obraId)),
                    onBack = ::goBack,
                    onOpenRelevamiento = { relevamiento ->
                        backStack.add(
                            RelevamientoDetailDestination(
                                obraId = destination.obraId,
                                relevamientoId = relevamiento.id
                            )
                        )
                    }
                )
            }
            entry<ObraDetailDestination> { destination ->
                ObraDetailRoute(
                    viewModel = viewModel(factory = container.obraDetailViewModelFactory(destination.obraId)),
                    onBack = ::goBack,
                    onEditObra = { obra ->
                        backStack.add(
                            ObraFormDestination(
                                obraId = obra.id,
                                nombre = obra.nombre,
                                cliente = obra.cliente,
                                direccion = obra.direccion,
                                estado = obra.estado.toStorageValue(),
                                publica = obra.publica,
                                latitud = obra.ubicacion?.latitud,
                                longitud = obra.ubicacion?.longitud
                            )
                        )
                    },
                    onCreateRelevamiento = {
                        backStack.add(RelevamientoFormDestination(obraId = destination.obraId))
                    },
                    onOpenRelevamiento = { relevamiento ->
                        backStack.add(
                            RelevamientoDetailDestination(
                                obraId = destination.obraId,
                                relevamientoId = relevamiento.id
                            )
                        )
                    }
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
                        publica = destination.publica,
                        latitud = destination.latitud,
                        longitud = destination.longitud
                    ),
                    onBack = ::goBack,
                    onSaved = ::goBack
                )
            }
            entry<RelevamientoDetailDestination> { destination ->
                RelevamientoDetailRoute(
                    viewModel = viewModel(
                        factory = container.relevamientoDetailViewModelFactory(
                            destination.obraId,
                            destination.relevamientoId
                        )
                    ),
                    onBack = ::goBack,
                    onEditRelevamiento = { relevamiento ->
                        backStack.add(
                            RelevamientoFormDestination(
                                obraId = destination.obraId,
                                relevamientoId = relevamiento.id,
                                nota = relevamiento.nota,
                                fechaMillis = relevamiento.fecha,
                                fotoUrl = relevamiento.fotoUrl,
                                latitud = relevamiento.ubicacion?.latitud,
                                longitud = relevamiento.ubicacion?.longitud
                            )
                        )
                    }
                )
            }
            entry<RelevamientoFormDestination> { destination ->
                RelevamientoFormRoute(
                    viewModel = viewModel(
                        factory = container.relevamientoFormViewModelFactory(
                            destination.obraId,
                            destination.relevamientoId
                        )
                    ),
                    initial = RelevamientoFormInitial(
                        obraId = destination.obraId,
                        relevamientoId = destination.relevamientoId,
                        nota = destination.nota,
                        fechaMillis = destination.fechaMillis,
                        fotoUrl = destination.fotoUrl,
                        latitud = destination.latitud,
                        longitud = destination.longitud
                    ),
                    onBack = ::goBack,
                    onSaved = ::goBack
                )
            }
        }
    )
}
