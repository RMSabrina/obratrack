package com.unaj.appmoviles.features.auth.profile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.semantics.Role
import com.unaj.appmoviles.obratrack.core.domain.model.ModoTema
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.unaj.appmoviles.obratrack.core.domain.ui.LoadingContent
import com.unaj.appmoviles.obratrack.core.domain.ui.asMessage

import androidx.compose.ui.res.stringResource
import com.unaj.appmoviles.auth.R
import com.unaj.appmoviles.obratrack.core.domain.model.AnticipacionAviso
import com.unaj.appmoviles.obratrack.core.domain.model.AppLanguage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    state: ProfileUiState,
    onNameChange: (String) -> Unit,
    onSaveName: () -> Unit,
    onShowChangePasswordDialog: () -> Unit,
    onHideChangePasswordDialog: () -> Unit,
    onChangePassword: (String) -> Unit,
    onShowDeleteAccountDialog: () -> Unit,
    onHideDeleteAccountDialog: () -> Unit,
    onConfirmDeleteAccount: () -> Unit,
    onHideReAuthDialog: () -> Unit,
    onReauthenticateAndRetry: (String) -> Unit,
    onClearMessage: () -> Unit,
    onClearError: () -> Unit,
    onLanguageChange: (AppLanguage) -> Unit,
    onTemaChange: (ModoTema) -> Unit,
    onAnticipacionChange: (AnticipacionAviso) -> Unit,
    onBack: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val snackbarHostState = remember { SnackbarHostState() }
    val user = state.user
    var showIdiomaDialog by rememberSaveable { mutableStateOf(false) }
    var showTemaDialog by rememberSaveable { mutableStateOf(false) }
    var showAvisoDialog by rememberSaveable { mutableStateOf(false) }
    var showLogoutDialog by rememberSaveable { mutableStateOf(false) }

    val errorMessage = state.error?.asMessage()
    val successMessage = state.successMessage?.let { stringResource(it) }

    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            snackbarHostState.showSnackbar(errorMessage)
            onClearError()
        }
    }

    LaunchedEffect(successMessage) {
        if (successMessage != null) {
            snackbarHostState.showSnackbar(successMessage)
            onClearMessage()
        }
    }

    if (showIdiomaDialog) {
        DialogoOpciones(
            titulo = stringResource(R.string.auth_idioma_titulo),
            opciones = AppLanguage.entries,
            seleccionada = state.selectedLanguage,
            etiqueta = { stringResource(it.labelRes()) },
            onElegir = onLanguageChange,
            onCerrar = { showIdiomaDialog = false }
        )
    }

    if (showTemaDialog) {
        DialogoOpciones(
            titulo = stringResource(R.string.auth_tema_titulo),
            opciones = ModoTema.entries,
            seleccionada = state.selectedTema,
            etiqueta = { stringResource(it.labelRes()) },
            onElegir = onTemaChange,
            onCerrar = { showTemaDialog = false }
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text(stringResource(R.string.auth_logout_confirmar_titulo)) },
            text = { Text(stringResource(R.string.auth_logout_confirmar_mensaje)) },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    onLogout()
                }) { Text(stringResource(R.string.auth_logout)) }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text(stringResource(R.string.auth_cancelar)) }
            }
        )
    }

    if (showAvisoDialog) {
        DialogoOpciones(
            titulo = stringResource(R.string.auth_aviso_titulo),
            opciones = AnticipacionAviso.entries,
            seleccionada = state.selectedAnticipacion,
            etiqueta = { stringResource(it.labelRes()) },
            onElegir = onAnticipacionChange,
            onCerrar = { showAvisoDialog = false }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.auth_perfil_titulo), fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        if (state.isLoading && user == null) {
            LoadingContent(Modifier.padding(padding))
        } else if (user != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EncabezadoPerfil(nombre = user.name, email = user.email)

                SeccionTitulo(stringResource(R.string.auth_seccion_cuenta))
                TarjetaSeccion {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = state.nameInput,
                            onValueChange = onNameChange,
                            label = { Text(stringResource(R.string.auth_nombre)) },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            enabled = !state.isSavingName
                        )
                        val isNameChanged = state.nameInput.trim() != user.name && state.nameInput.isNotBlank()
                        Button(
                            onClick = onSaveName,
                            modifier = Modifier.align(Alignment.End),
                            enabled = isNameChanged && !state.isSavingName
                        ) {
                            if (state.isSavingName) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(stringResource(R.string.auth_guardar_cambios))
                            }
                        }
                    }
                    HorizontalDivider()
                    FilaPerfil(
                        titulo = stringResource(R.string.auth_cambiar_password),
                        icono = Icons.Default.Lock,
                        onClick = onShowChangePasswordDialog
                    )
                }

                SeccionTitulo(stringResource(R.string.auth_seccion_preferencias))
                TarjetaSeccion {
                    FilaPerfil(
                        titulo = stringResource(R.string.auth_idioma_titulo),
                        valor = stringResource(state.selectedLanguage.labelRes()),
                        onClick = { showIdiomaDialog = true }
                    )
                    HorizontalDivider()
                    FilaPerfil(
                        titulo = stringResource(R.string.auth_tema_titulo),
                        valor = stringResource(state.selectedTema.labelRes()),
                        onClick = { showTemaDialog = true }
                    )
                    HorizontalDivider()
                    FilaPerfil(
                        titulo = stringResource(R.string.auth_aviso_titulo),
                        valor = stringResource(state.selectedAnticipacion.labelRes()),
                        onClick = { showAvisoDialog = true }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { showLogoutDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(stringResource(R.string.auth_logout), fontWeight = FontWeight.Bold)
                }

                // La acción destructiva va aparte, al final y sin parecer un botón principal.
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                TextButton(
                    onClick = onShowDeleteAccountDialog,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.auth_eliminar_cuenta),
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    // Diálogo: Cambiar Contraseña
    if (state.showChangePasswordDialog) {
        ChangePasswordDialog(
            isChangingPassword = state.isChangingPassword,
            onDismiss = onHideChangePasswordDialog,
            onConfirm = onChangePassword
        )
    }

    // Diálogo: Confirmar Eliminar Cuenta
    if (state.showDeleteAccountDialog) {
        DeleteAccountConfirmationDialog(
            isDeletingAccount = state.isDeletingAccount,
            onDismiss = onHideDeleteAccountDialog,
            onConfirm = onConfirmDeleteAccount
        )
    }

    // Diálogo: Reautenticación por Sesión Reciente Requerida
    if (state.showReAuthDialog) {
        ReAuthDialog(
            isReauthenticating = state.isReauthenticating,
            onDismiss = onHideReAuthDialog,
            onConfirm = onReauthenticateAndRetry
        )
    }
}

/** Avatar con las iniciales, el nombre y el correo (solo lectura) con su marca de verificado. */
@Composable
private fun EncabezadoPerfil(nombre: String, email: String) {
    val initials = nombre
        .split(" ")
        .mapNotNull { it.firstOrNull()?.uppercase() }
        .take(2)
        .joinToString("")
        .ifEmpty { "U" }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = nombre,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = email,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = stringResource(R.string.auth_verificado),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun SeccionTitulo(texto: String) {
    Text(
        text = texto,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .padding(start = 4.dp, top = 8.dp)
            .semantics { heading() }
    )
}

@Composable
private fun TarjetaSeccion(contenido: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        content = contenido
    )
}

/** Fila táctil (mínimo 56 dp): título, el valor actual debajo si hay, y un ícono al final. */
@Composable
private fun FilaPerfil(
    titulo: String,
    onClick: () -> Unit,
    valor: String? = null,
    icono: ImageVector? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (icono != null) {
            Icon(imageVector = icono, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (valor != null) {
                Text(
                    text = valor,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Diálogo con una opción por fila (radio). Elegir una aplica el cambio y cierra. */
@Composable
private fun <T> DialogoOpciones(
    titulo: String,
    opciones: List<T>,
    seleccionada: T,
    etiqueta: @Composable (T) -> String,
    onElegir: (T) -> Unit,
    onCerrar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text(titulo) },
        text = {
            Column(modifier = Modifier.selectableGroup()) {
                opciones.forEach { opcion ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .selectable(
                                selected = seleccionada == opcion,
                                onClick = {
                                    onElegir(opcion)
                                    onCerrar()
                                },
                                role = Role.RadioButton
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = seleccionada == opcion, onClick = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(etiqueta(opcion))
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onCerrar) { Text(stringResource(R.string.auth_cancelar)) }
        }
    )
}

private fun AppLanguage.labelRes(): Int = when (this) {
    AppLanguage.SPANISH -> R.string.auth_idioma_espanol
    AppLanguage.ENGLISH -> R.string.auth_idioma_ingles
    AppLanguage.ITALIAN -> R.string.auth_idioma_italiano
}

@Composable
private fun ChangePasswordDialog(
    isChangingPassword: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newPassword by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.auth_cambiar_password)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.auth_nueva_password_instruccion))
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text(stringResource(R.string.auth_nueva_password)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(newPassword) },
                enabled = newPassword.length >= 6 && !isChangingPassword
            ) {
                if (isChangingPassword) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.auth_cambiar))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isChangingPassword) {
                Text(stringResource(R.string.auth_cancelar))
            }
        }
    )
}

@Composable
private fun DeleteAccountConfirmationDialog(
    isDeletingAccount: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.auth_eliminar_cuenta_titulo)) },
        text = {
            Text(stringResource(R.string.auth_eliminar_cuenta_mensaje))
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isDeletingAccount,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                if (isDeletingAccount) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.auth_eliminar_definitivamente))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isDeletingAccount) {
                Text(stringResource(R.string.auth_cancelar))
            }
        }
    )
}

@Composable
private fun ReAuthDialog(
    isReauthenticating: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.auth_reauth_titulo)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.auth_reauth_mensaje))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.auth_password_actual)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(password) },
                enabled = password.isNotBlank() && !isReauthenticating
            ) {
                if (isReauthenticating) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.auth_confirmar))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isReauthenticating) {
                Text(stringResource(R.string.auth_cancelar))
            }
        }
    )
}

private fun AnticipacionAviso.labelRes(): Int = when (this) {
    AnticipacionAviso.AL_MOMENTO -> R.string.auth_aviso_al_momento
    AnticipacionAviso.MIN_15 -> R.string.auth_aviso_15_min
    AnticipacionAviso.HORA_1 -> R.string.auth_aviso_1_hora
    AnticipacionAviso.DIA_1 -> R.string.auth_aviso_1_dia
}

private fun ModoTema.labelRes(): Int = when (this) {
    ModoTema.SISTEMA -> R.string.auth_tema_sistema
    ModoTema.CLARO -> R.string.auth_tema_claro
    ModoTema.OSCURO -> R.string.auth_tema_oscuro
}
