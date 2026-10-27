package com.unaj.appmoviles.features.auth.login

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.unaj.appmoviles.auth.R
import com.unaj.appmoviles.features.auth.components.AuthTextField
import com.unaj.appmoviles.features.auth.google.GoogleSignInHelper
import com.unaj.appmoviles.features.auth.google.GoogleSignInResult
import com.unaj.appmoviles.obratrack.core.domain.error.AuthError
import com.unaj.appmoviles.obratrack.core.domain.error.DomainError
import com.unaj.appmoviles.obratrack.core.domain.ui.asMessage
import kotlinx.coroutines.launch

@Composable
fun LoginRoute(
    viewModel: LoginViewModel = hiltViewModel(),
    onNavigateToRegister: () -> Unit,
    /** Se tocó una notificación de obra sin sesión: después del login se abre esa obra. */
    mostrarAvisoObraPendiente: Boolean = false
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    LoginScreen(
        state = state,
        onLoginClick = viewModel::onLoginClick,
        onGoogleClick = {
            coroutineScope.launch {
                when (val result = GoogleSignInHelper.signIn(context)) {
                    is GoogleSignInResult.Success -> viewModel.onGoogleLoginClick(result.idToken)
                    is GoogleSignInResult.Error -> viewModel.onGoogleLoginError(result.error)
                    GoogleSignInResult.Cancelled -> Unit
                }
            }
        },
        onInputChanged = viewModel::onErrorShown,
        onRegisterClick = onNavigateToRegister,
        mostrarAvisoObraPendiente = mostrarAvisoObraPendiente
    )
}

@Composable
fun LoginScreen(
    state: LoginUiState,
    onLoginClick: (email: String, password: String) -> Unit,
    onGoogleClick: () -> Unit,
    onInputChanged: () -> Unit,
    onRegisterClick: () -> Unit,
    modifier: Modifier = Modifier,
    mostrarAvisoObraPendiente: Boolean = false
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    val error: DomainError? = when (state) {
        LoginUiState.Idle, LoginUiState.Loading -> null
        is LoginUiState.Error -> state.error
    }
    val isLoading = state is LoginUiState.Loading

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Logo y Título
        Icon(
            imageVector = Icons.Default.Build,
            contentDescription = "Logo",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "ObraTrack",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.auth_login_subtitulo),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (mostrarAvisoObraPendiente) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.auth_aviso_obra_notificacion),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))

        AuthTextField(
            value = email,
            onValueChange = { email = it; onInputChanged() },
            label = stringResource(R.string.auth_campo_email),
            error = if (error == AuthError.InvalidEmail) error.asMessage() else null,
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = Color.Gray) },
            keyboardType = KeyboardType.Email
        )
        Spacer(Modifier.height(8.dp))
        AuthTextField(
            value = password,
            onValueChange = { password = it; onInputChanged() },
            label = stringResource(R.string.auth_campo_password),
            error = if (error == AuthError.PasswordTooShort) error.asMessage() else null,
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color.Gray) },
            isPassword = true
        )

        // Errores que no pertenecen a un campo
        if (error != null && error != AuthError.InvalidEmail && error != AuthError.PasswordTooShort) {
            Text(error.asMessage(), color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
        }

        Spacer(Modifier.height(24.dp))
        
        Button(
            onClick = { onLoginClick(email, password) },
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            if (isLoading) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
            else Text(stringResource(R.string.auth_iniciar_sesion), fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = onGoogleClick,
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_google),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.auth_continuar_google), fontWeight = FontWeight.Bold)
        }
        
        Spacer(Modifier.height(16.dp))
        
        TextButton(onClick = onRegisterClick) {
            Text(stringResource(R.string.auth_sin_cuenta), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(stringResource(R.string.auth_registrate), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginScreenPreview() = MaterialTheme {
    LoginScreen(LoginUiState.Error(AuthError.InvalidCredentials), { _, _ -> }, {}, {}, {})
}
