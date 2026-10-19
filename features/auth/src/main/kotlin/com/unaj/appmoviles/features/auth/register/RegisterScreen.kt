package com.unaj.appmoviles.features.auth.register

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.unaj.appmoviles.features.auth.components.AuthTextField
import com.unaj.appmoviles.obratrack.core.domain.error.AuthError
import com.unaj.appmoviles.obratrack.core.domain.error.DomainError
import com.unaj.appmoviles.obratrack.core.domain.ui.asMessage

import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun RegisterRoute(viewModel: RegisterViewModel = hiltViewModel(), onNavigateBack: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    RegisterScreen(
        state = state,
        onRegisterClick = viewModel::onRegisterClick,
        onInputChanged = viewModel::onErrorShown,
        onBackToLogin = onNavigateBack,
        onResendVerification = viewModel::onResendVerification
    )
}

@Composable
fun RegisterScreen(
    state: RegisterUiState,
    onRegisterClick: (name: String, email: String, password: String) -> Unit,
    onInputChanged: () -> Unit,
    onBackToLogin: () -> Unit,
    modifier: Modifier = Modifier,
    onResendVerification: () -> Unit = {}
) {
    if (state is RegisterUiState.VerificationSent) {
        EmailVerificationSentScreen(
            email = state.email,
            isResending = state.isResending,
            resendSent = state.resendSent,
            resendError = state.resendError,
            onBackToLogin = onBackToLogin,
            onResend = onResendVerification
        )
        return
    }

    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var mismatch by rememberSaveable { mutableStateOf(false) }

    val error: DomainError? = when (state) {
        RegisterUiState.Idle, RegisterUiState.Loading -> null
        is RegisterUiState.VerificationSent -> null
        is RegisterUiState.Error -> state.error
    }
    val isLoading = state is RegisterUiState.Loading
    val fieldErrors = setOf(
        AuthError.BlankName, AuthError.InvalidEmail,
        AuthError.EmailAlreadyInUse, AuthError.PasswordTooShort
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "Crear cuenta",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Completá tus datos para registrarte",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))

        AuthTextField(
            value = name,
            onValueChange = { name = it; onInputChanged() },
            label = "Nombre",
            error = if (error == AuthError.BlankName) error.asMessage() else null,
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color.Gray) }
        )
        Spacer(Modifier.height(8.dp))
        AuthTextField(
            value = email,
            onValueChange = { email = it; onInputChanged() },
            label = "Email",
            error = if (error == AuthError.InvalidEmail || error == AuthError.EmailAlreadyInUse) error.asMessage() else null,
            keyboardType = KeyboardType.Email,
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = Color.Gray) }
        )
        Spacer(Modifier.height(8.dp))
        AuthTextField(
            value = password,
            onValueChange = { password = it; mismatch = false; onInputChanged() },
            label = "Contraseña",
            error = if (error == AuthError.PasswordTooShort) error.asMessage() else null,
            isPassword = true,
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color.Gray) }
        )
        Spacer(Modifier.height(8.dp))
        AuthTextField(
            value = confirm,
            onValueChange = { confirm = it; mismatch = false },
            label = "Confirmar contraseña",
            error = if (mismatch) "Las contraseñas no coinciden" else null,
            isPassword = true,
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color.Gray) }
        )

        if (error != null && error !in fieldErrors) {
            Text(error.asMessage(), color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
        }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                if (password != confirm) mismatch = true
                else onRegisterClick(name, email, password)
            },
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            if (isLoading) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
            else Text("Registrarse", fontWeight = FontWeight.Bold)
        }
        
        Spacer(Modifier.height(16.dp))
        
        TextButton(onClick = onBackToLogin, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("¿Ya tenés cuenta? ", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Iniciá sesión", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun EmailVerificationSentScreen(
    email: String,
    isResending: Boolean,
    resendSent: Boolean,
    resendError: DomainError?,
    onBackToLogin: () -> Unit,
    onResend: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("¡Cuenta creada!", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))
        Text(
            "Te enviamos un correo a $email para verificar tu cuenta. " +
                    "Revisá tu bandeja (y la carpeta de spam) antes de iniciar sesión.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onBackToLogin, modifier = Modifier.fillMaxWidth()) {
            Text("Volver al login")
        }
        TextButton(onClick = onResend, enabled = !isResending) {
            if (isResending) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
            else Text("Reenviar correo")
        }
        if (resendSent) {
            Text("Correo reenviado", style = MaterialTheme.typography.bodySmall)
        }
        if (resendError != null) {
            Text(
                resendError.asMessage(),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}