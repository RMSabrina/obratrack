package com.unaj.appmoviles.obratrack.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.NavKey
import com.unaj.appmoviles.obratrack.R

private class Pestana(val destino: NavKey, val icono: ImageVector, val etiqueta: Int)

private val pestanas = listOf(
    Pestana(HomeDestination, Icons.Outlined.Home, R.string.nav_inicio),
    Pestana(ObrasDestination, Icons.Outlined.Business, R.string.nav_obras),
    Pestana(AgendaDestination, Icons.Outlined.CalendarMonth, R.string.nav_agenda),
    Pestana(ProfileDestination, Icons.Outlined.Person, R.string.nav_perfil)
)

/** Barra de las cuatro pestañas; la definición es única y la muestra `AppShell` solo en las de nivel superior. */
@Composable
fun BarraInferior(seleccionado: NavKey, onSeleccionar: (NavKey) -> Unit) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.primary
    ) {
        pestanas.forEach { pestana ->
            val activa = pestana.destino == seleccionado
            NavigationBarItem(
                selected = activa,
                onClick = { onSeleccionar(pestana.destino) },
                icon = { Icon(pestana.icono, contentDescription = stringResource(pestana.etiqueta)) },
                label = { Text(stringResource(pestana.etiqueta)) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
