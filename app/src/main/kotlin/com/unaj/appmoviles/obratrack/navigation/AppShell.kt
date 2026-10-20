package com.unaj.appmoviles.obratrack.navigation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
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
import com.unaj.appmoviles.obratrack.notification.NotificationHelper
import com.unaj.appmoviles.obratrack.worker.RelevamientoReminderWorker

@Composable
fun AppShell(onLogout: () -> Unit) {
    val backStack = rememberNavBackStack(HomeDestination)
    val appContext = LocalContext.current

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        NotificationHelper.showDemoNotification(appContext)
        RelevamientoReminderWorker.triggerImmediateDemo(appContext)
    }

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
                    },
                    onTriggerDemoNotification = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            if (ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                                NotificationHelper.showDemoNotification(appContext)
                                RelevamientoReminderWorker.triggerImmediateDemo(appContext)
                            } else {
                                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        } else {
                            NotificationHelper.showDemoNotification(appContext)
                            RelevamientoReminderWorker.triggerImmediateDemo(appContext)
                        }
                    }
                )
            }
            entry<ObraSelectionDestination> {
                ObraSelectionRoute(
                    onObraSelected = { obra ->
                        backStack.removeAt(backStack.lastIndex)
                        backStack.add(RelevamientoFormDestination(obraId = obra.id))
                    },
                    onBack = ::goBack
                )
            }
            entry<ProfileDestination> {
                ProfileRoute(
                    onBack = ::goBack,
                    onLogout = onLogout
                )
            }
            entry<ObrasDestination> {
                ObrasListRoute(
                    onCreate = { backStack.add(ObraFormDestination()) },
                    onOpen = { obra -> backStack.add(ObraDetailDestination(obra.id)) },
                    onLogout = onLogout,
                    onBack = ::goBack
                )
            }
            entry<ObrasPublicasDestination> {
                ObrasPublicasListRoute(
                    onOpen = { obra -> backStack.add(ObraPublicaDetailDestination(obra.id)) },
                    onBack = ::goBack
                )
            }
            entry<ObraPublicaDetailDestination> { destination ->
                ObraPublicaDetailRoute(
                    obraId = destination.obraId,
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
                    obraId = destination.obraId,
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
                                longitud = obra.ubicacion?.longitud,
                                proximaVisita = obra.proximaVisita
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
                    initial = ObraFormInitial(
                        obraId = destination.obraId,
                        nombre = destination.nombre,
                        cliente = destination.cliente,
                        direccion = destination.direccion,
                        estado = destination.estado,
                        publica = destination.publica,
                        latitud = destination.latitud,
                        longitud = destination.longitud,
                        proximaVisita = destination.proximaVisita
                    ),
                    onBack = ::goBack,
                    onSaved = ::goBack
                )
            }
            entry<RelevamientoDetailDestination> { destination ->
                RelevamientoDetailRoute(
                    obraId = destination.obraId,
                    relevamientoId = destination.relevamientoId,
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
                                longitud = relevamiento.ubicacion?.longitud,
                                proximaVisita = relevamiento.proximaVisita
                            )
                        )
                    }
                )
            }
            entry<RelevamientoFormDestination> { destination ->
                RelevamientoFormRoute(
                    initial = RelevamientoFormInitial(
                        obraId = destination.obraId,
                        relevamientoId = destination.relevamientoId,
                        nota = destination.nota,
                        fechaMillis = destination.fechaMillis,
                        fotoUrl = destination.fotoUrl,
                        latitud = destination.latitud,
                        longitud = destination.longitud,
                        proximaVisita = destination.proximaVisita
                    ),
                    onBack = ::goBack,
                    onSaved = ::goBack
                )
            }
        }
    )
}
