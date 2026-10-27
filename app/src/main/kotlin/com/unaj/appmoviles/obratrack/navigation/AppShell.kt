package com.unaj.appmoviles.obratrack.navigation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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

@Composable
fun AppShell(
    onLogout: () -> Unit,
    /** Obra que pidió abrir una notificación; null si no hay nada pendiente. */
    obraAAbrir: String? = null,
    onObraAbierta: () -> Unit = {}
) {
    val backStack = rememberNavBackStack(HomeDestination)
    val appContext = LocalContext.current

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
        backStack.navegarA(destination)
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

    // La barra se define una sola vez, aquí: se ve en las pantallas de nivel superior y se oculta en el resto.
    val actual = backStack.lastOrNull()
    val conBarra = actual?.esDeNivelSuperior() == true
    // Pestaña que se marca; se recuerda para que no cambie mientras la barra se oculta con animación.
    var pestanaMarcada by remember { mutableStateOf<NavKey>(HomeDestination) }
    if (actual != null && conBarra) pestanaMarcada = actual

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            AnimatedVisibility(
                visible = conBarra,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                BarraInferior(
                    seleccionado = pestanaMarcada,
                    onSeleccionar = { backStack.cambiarDePestana(it) }
                )
            }
        }
    ) { innerPadding ->
    NavDisplay(
        modifier = Modifier.padding(innerPadding).consumeWindowInsets(innerPadding),
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
                    }
                )
            }
            entry<AgendaDestination> {
                AgendaRoute(
                    onBack = ::popToHome,
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
                    onOpen = { obra -> navigateTo(ObraPublicaDetailDestination(obra.id)) },
                    onBack = ::goBack
                )
            }
            entry<ObraPublicaDetailDestination> { destination ->
                ObraPublicaDetailRoute(
                    obraId = destination.obraId,
                    onBack = ::goBack,
                    onOpenRelevamiento = { relevamiento ->
                        navigateTo(
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
                        navigateTo(
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
                        navigateTo(RelevamientoFormDestination(obraId = destination.obraId))
                    },
                    onOpenRelevamiento = { relevamiento ->
                        navigateTo(
                            RelevamientoDetailDestination(
                                obraId = destination.obraId,
                                relevamientoId = relevamiento.id
                            )
                        )
                    },
                    onCreateTarea = {
                        navigateTo(TareaFormDestination(obraId = destination.obraId))
                    },
                    onOpenTarea = { tareaId ->
                        navigateTo(TareaDetailDestination(obraId = destination.obraId, tareaId = tareaId))
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
                        navigateTo(
                            TareaFormDestination(obraId = destination.obraId, tareaId = destination.tareaId)
                        )
                    },
                    onOpenRelevamiento = { relevamientoId ->
                        navigateTo(
                            RelevamientoDetailDestination(
                                obraId = destination.obraId,
                                relevamientoId = relevamientoId
                            )
                        )
                    },
                    onCreateRelevamiento = {
                        navigateTo(
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
                        navigateTo(
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
}
