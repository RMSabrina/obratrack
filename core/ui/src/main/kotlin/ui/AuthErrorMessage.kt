package com.unaj.appmoviles.obratrack.core.domain.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.unaj.appmoviles.obratrack.core.domain.error.AuthError
import com.unaj.appmoviles.obratrack.core.domain.error.DomainError
import com.unaj.appmoviles.obratrack.core.domain.error.ObraError
import com.unaj.appmoviles.obratrack.core.domain.error.RelevamientoError
import com.unaj.appmoviles.obratrack.core.domain.error.TareaError
import com.unaj.appmoviles.obratrack.core.ui.R

@Composable
fun DomainError.asMessage(): String = stringResource(
    when (this) {
        AuthError.InvalidEmail -> R.string.error_invalid_email
        AuthError.PasswordTooShort -> R.string.error_password_too_short
        AuthError.BlankName -> R.string.error_blank_name
        AuthError.InvalidCredentials -> R.string.error_invalid_credentials
        AuthError.EmailAlreadyInUse -> R.string.error_email_in_use
        AuthError.NetworkError -> R.string.error_network
        AuthError.RequiresRecentLogin -> R.string.error_recent_login_required
        AuthError.EmailNotVerified -> R.string.error_email_not_verified
        AuthError.GoogleConfigurationError -> R.string.error_google_config
        AuthError.GoogleSignInFailed -> R.string.error_google_failed
        AuthError.GoogleNoAccounts -> R.string.error_google_no_accounts
        ObraError.NombreVacio -> R.string.error_obra_nombre
        ObraError.ClienteVacio -> R.string.error_obra_cliente
        ObraError.DireccionVacia -> R.string.error_obra_direccion
        ObraError.NoEncontrada -> R.string.error_obra_not_found
        ObraError.NoAutorizada -> R.string.error_obra_forbidden
        ObraError.Red -> R.string.error_network
        ObraError.Timeout -> R.string.error_timeout
        ObraError.FalloParcial -> R.string.error_fallo_parcial
        RelevamientoError.NotaVacia -> R.string.error_relevamiento_nota
        RelevamientoError.FechaInvalida -> R.string.error_relevamiento_fecha
        RelevamientoError.NoEncontrado -> R.string.error_relevamiento_not_found
        RelevamientoError.NoAutorizado -> R.string.error_relevamiento_forbidden
        RelevamientoError.ImagenMuyGrande -> R.string.error_relevamiento_imagen_grande
        RelevamientoError.Red -> R.string.error_network
        RelevamientoError.Timeout -> R.string.error_timeout
        TareaError.TituloVacio -> R.string.error_tarea_titulo
        TareaError.FechasIncompletas -> R.string.error_tarea_fechas_incompletas
        TareaError.FechaFinAnterior -> R.string.error_tarea_fecha_fin_anterior
        TareaError.NoEncontrada -> R.string.error_tarea_not_found
        TareaError.NoAutorizada -> R.string.error_tarea_forbidden
        TareaError.Red -> R.string.error_network
        TareaError.Timeout -> R.string.error_tarea_timeout
        else -> R.string.error_unknown
    }
)