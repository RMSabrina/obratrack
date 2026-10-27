package com.unaj.appmoviles.obratrack.navigation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
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
import com.unaj.appmoviles.features.obras.tareas.detail.TareaDetailRoute
import com.unaj.appmoviles.features.obras.tareas.form.TareaFormRoute
import com.unaj.appmoviles.features.home.HomeRoute
import com.unaj.appmoviles.features.home.ObraSelectionRoute
import com.unaj.appmoviles.features.home.agenda.AgendaRoute
import com.unaj.appmoviles.obratrack.core.domain.model.Relevamiento
import com.unaj.appmoviles.features.obras.toStorageValue
import com.unaj.appmoviles.obratrack.notification.NotificationHelper
import com.unaj.appmoviles.obratrack.worker.RelevamientoReminderWorker

@Composable
fun AppShell(
    onLogout: () -> Unit,
    /** Obra que pidió abrir una notificación; null si no hay nada pendiente. */
    obraAAbrir: String? = null,
    onObraAbierta: () -> Unit = {}
) {
    val backStack = rememberNavBackStack(HomeDestination)
    val appContext = LocalContext.current

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        NotificationHelper.showDemoNotification(appContext)
        RelevamientoReminderWorker.triggerImmediateDemo(appContext)
    }

    // Android 13+: sin este permiso los avisos de visita no se muestran.
    val avisosPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            avisosPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    fun goBack() {
        if (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
        } else {
            backStack.clear()
            backStack.add(HomeDestination)
        }
    }

    fun navigateTo(destination: NavKey) {
        if (backStack.lastOrNull() != destination) {
            backStack.add(destination)
        }
    }

    fun popToHome() {
        while (backStack.size > 1 && backStack.last() !is HomeDestination) {
            backStack.removeAt(backStack.lastIndex)
        }
        if (backStack.isEmpty() || backStack.first() !is HomeDestination) {
            backStack.clear()
            backStack.add(HomeDestination)
        }
    }

    // Se apila el detalle sin vaciar el back stack (no se pierde un formulario abierto); con la
    // app cerrada queda [Home, ObraDetail]. Navegar y consumir van juntos, sin suspender entre
    // ambos: cada intent navega una sola vez. Si arriba ya está esa obra, navigateTo no hace nada.
    LaunchedEffect(obraAAbrir) {
        val obraId = obraAAbrir ?: return@LaunchedEffect
        navigateTo(ObraDetailDestination(obraId))
        onObraAbierta()
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
                    onNavigateToObras = { navigateTo(ObrasDestination) },
                    onNavigateToAgenda = { navigateTo(AgendaDestination) },
                    onNavigateToObraSelection = { navigateTo(ObraSelectionDestination) },
                    onNavigateToObrasPublicas = { navigateTo(ObrasPublicasDestination) },
                    onNavigateToProfile = { navigateTo(ProfileDestination) },
                    onIniciarRelevamientoParaObra = { obraId ->
                        navigateTo(RelevamientoFormDestination(obraId = obraId))
                    },
                    onOpenRelevamiento = { relevamiento ->
                        navigateTo(
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
            entry<AgendaDestination> {
                AgendaRoute(
                    onBack = ::popToHome,
                    onNavigateToHome = ::popToHome,
                    onNavigateToObras = {
                        popToHome()
                        navigateTo(ObrasDestination)
                    },
                    onNavigateToProfile = {
                        popToHome()
                        navigateTo(ProfileDestination)
                    },
                    onOpenObra = { obraId ->
                        navigateTo(ObraDetailDestination(obraId))
                    },
                    onIniciarRelevamiento = { obraId ->
                        navigateTo(RelevamientoFormDestination(obraId = obraId))
                    }
                )
            }
            entry<ObraSelectionDestination> {
                ObraSelectionRoute(
                    onObraSelected = { obra ->
                        backStack.removeAt(backStack.lastIndex)
                        navigateTo(RelevamientoFormDestination(obraId = obra.id))
                    },
                    onBack = ::goBack
                )
            }
            entry<ProfileDestination> {
                ProfileRoute(
                    onBack = ::popToHome,
                    onLogout = onLogout
                )
            }
            entry<ObrasDestination> {
                ObrasListRoute(
                    onCreate = { navigateTo(ObraFormDestination()) },
                    onOpen = { obra -> navigateTo(ObraDetailDestination(obra.id)) },
                    onLogout = onLogout,
                    onBack = ::popToHome
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
                    onDeleteSuccess = {
                        while (backStack.size > 1 && backStack.last() !is ObrasDestination && backStack.last() !is HomeDestination) {
                            backStack.removeAt(backStack.lastIndex)
                        }
                    },
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
                                proximaVisita = obra.proximaVisita,
                                horaVisita = obra.horaVisita
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
                    },
                    onCreateTarea = {
                        backStack.add(TareaFormDestination(obraId = destination.obraId))
                    },
                    onOpenTarea = { tareaId ->
                        backStack.add(TareaDetailDestination(obraId = destination.obraId, tareaId = tareaId))
                    }
                )
            }
            entry<TareaDetailDestination> { destination ->
                TareaDetailRoute(
                    obraId = destination.obraId,
                    tareaId = destination.tareaId,
                    onBack = ::goBack,
                    onDeleteSuccess = ::goBack,
                    onEditTarea = {
                        backStack.add(
                            TareaFormDestination(obraId = destination.obraId, tareaId = destination.tareaId)
                        )
                    },
                    onOpenRelevamiento = { relevamientoId ->
                        backStack.add(
                            RelevamientoDetailDestination(
                                obraId = destination.obraId,
                                relevamientoId = relevamientoId
                            )
                        )
                    },
                    onCreateRelevamiento = {
                        backStack.add(
                            RelevamientoFormDestination(
                                obraId = destination.obraId,
                                tareaId = destination.tareaId
                            )
                        )
                    }
                )
            }
            entry<TareaFormDestination> { destination ->
                TareaFormRoute(
                    obraId = destination.obraId,
                    tareaId = destination.tareaId,
                    onBack = ::goBack,
                    // Al guardar se vuelve al detalle de obra (en la pestaña de origen, Tareas o
                    // Cronograma), aunque la edición se haya abierto desde el detalle de la tarea.
                    onSaved = {
                        while (backStack.size > 1 && backStack.last() !is ObraDetailDestination) {
                            backStack.removeAt(backStack.lastIndex)
                        }
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
                        proximaVisita = destination.proximaVisita,
                        horaVisita = destination.horaVisita
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
                    onDeleteSuccess = ::goBack,
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
                                proximaVisita = relevamiento.proximaVisita,
                                horaVisita = relevamiento.horaVisita,
                                tareaId = relevamiento.tareaId
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
                        proximaVisita = destination.proximaVisita,
                        horaVisita = destination.horaVisita,
                        tareaId = destination.tareaId
                    ),
                    onBack = ::goBack,
                    onSaved = ::goBack
                )
            }
        }
    )
}
