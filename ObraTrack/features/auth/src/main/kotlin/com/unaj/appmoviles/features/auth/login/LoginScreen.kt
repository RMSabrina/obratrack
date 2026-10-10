package com.unaj.appmoviles.features.auth.login

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.unaj.appmoviles.features.auth.components.AuthTextField
import com.unaj.appmoviles.obratrack.core.domain.error.AuthError
import com.unaj.appmoviles.obratrack.core.domain.error.DomainError
import com.unaj.appmoviles.obratrack.core.domain.ui.asMessage


@Composable
fun LoginRoute(
    viewModel: LoginViewModel,
    onNavigateToRegister: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LoginScreen(
        state = state,
        onLoginClick = viewModel::onLoginClick,
        onInputChanged = viewModel::onErrorShown,
        onRegisterClick = onNavigateToRegister
    )
}

@Composable
fun LoginScreen(
    state: LoginUiState,
    onLoginClick: (email: String, password: String) -> Unit,
    onInputChanged: () -> Unit,
    onRegisterClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    val error: DomainError? = when (state) {
        LoginUiState.Idle, LoginUiState.Loading -> null
        is LoginUiState.Error -> state.error
    }
    val isLoading = state is LoginUiState.Loading

    Column(modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        AuthTextField(
            value = email,
            onValueChange = { email = it; onInputChanged() },
            label = "Email",
            error = if (error == AuthError.InvalidEmail) error.asMessage() else null,
            keyboardType = KeyboardType.Email
        )
        Spacer(Modifier.height(8.dp))
        AuthTextField(
            value = password,
            onValueChange = { password = it; onInputChanged() },
            label = "Contraseña",
            error = if (error == AuthError.PasswordTooShort) error.asMessage() else null,
            isPassword = true
        )

        // Errores que no pertenecen a un campo
        if (error != null && error != AuthError.InvalidEmail && error != AuthError.PasswordTooShort) {
            Text(error.asMessage(), color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
        }

        Spacer(Modifier.height(16.dp))
        Button(onClick = { onLoginClick(email, password) }, enabled = !isLoading, modifier = Modifier.fillMaxWidth()) {
            if (isLoading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            else Text("Iniciar sesión")
        }
        TextButton(onClick = onRegisterClick, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("¿No tenés cuenta? Registrate")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginScreenPreview() = MaterialTheme {
    LoginScreen(LoginUiState.Error(AuthError.InvalidCredentials), { _, _ -> }, {}, {})
}