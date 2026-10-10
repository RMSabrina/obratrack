package com.unaj.appmoviles.obratrack.navigation

import AppContainer
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import com.unaj.appmoviles.features.auth.login.LoginRoute
import com.unaj.appmoviles.features.auth.register.RegisterRoute

@Composable
fun AuthNavigation(container: AppContainer) {
    val backStack = rememberNavBackStack(LoginDestination)
    fun goBack() { backStack.removeAt(backStack.lastIndex) }

    NavDisplay(
        backStack = backStack,
        onBack = ::goBack,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        entryProvider = entryProvider {
            entry<LoginDestination> {
                LoginRoute(
                    viewModel = viewModel(factory = container.loginViewModelFactory),
                    onNavigateToRegister = { backStack.add(RegisterDestination) }
                )
            }
            entry<RegisterDestination> {
                RegisterRoute(
                    viewModel = viewModel(factory = container.registerViewModelFactory),
                    onNavigateBack = ::goBack
                )
            }
        }
    )
}