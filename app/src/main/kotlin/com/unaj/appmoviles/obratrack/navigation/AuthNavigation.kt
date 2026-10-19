package com.unaj.appmoviles.obratrack.navigation

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
fun AuthNavigation() {
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
                    onNavigateToRegister = { backStack.add(RegisterDestination) }
                )
            }
            entry<RegisterDestination> {
                RegisterRoute(
                    onNavigateBack = ::goBack
                )
            }
        }
    )
}