package com.unaj.appmoviles.obratrack.core.domain.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.unaj.appmoviles.obratrack.core.domain.error.AuthError
import com.unaj.appmoviles.obratrack.core.domain.error.DomainError
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
        else -> R.string.error_unknown
    }
)