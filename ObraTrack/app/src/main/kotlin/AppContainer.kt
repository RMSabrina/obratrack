import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.google.firebase.auth.FirebaseAuth
import com.unaj.appmoviles.features.auth.login.LoginViewModel
import com.unaj.appmoviles.features.auth.register.RegisterViewModel
import com.unaj.appmoviles.obratrack.core.data.repository.AuthRepositoryImpl
import com.unaj.appmoviles.obratrack.core.domain.repository.AuthRepository
import com.unaj.appmoviles.obratrack.core.domain.usecase.LoginUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.LogoutUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.ObserveSessionUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.RegisterUseCase
import com.unaj.appmoviles.obratrack.core.firebase.FirebaseAuthDataSourceImpl
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppContainer {
    val authRepository: AuthRepository =
        AuthRepositoryImpl(FirebaseAuthDataSourceImpl(FirebaseAuth.getInstance()))

    private val loginUseCase = LoginUseCase(authRepository)
    private val registerUseCase = RegisterUseCase(authRepository)
    private val logoutUseCase = LogoutUseCase(authRepository)
    private val observeSessionUseCase = ObserveSessionUseCase(authRepository)

    val loginViewModelFactory = viewModelFactory { initializer { LoginViewModel(loginUseCase) } }
    val registerViewModelFactory = viewModelFactory { initializer {
        RegisterViewModel(
            registerUseCase
        )
    } }
    val sessionViewModelFactory = viewModelFactory {
        initializer { SessionViewModel(observeSessionUseCase, logoutUseCase) }
    }
}

sealed interface SessionUiState {
    data object Loading : SessionUiState
    data object Authenticated : SessionUiState
    data object Unauthenticated : SessionUiState
}

class SessionViewModel(
    observeSession: ObserveSessionUseCase,
    private val logoutUseCase: LogoutUseCase
) : ViewModel() {

    val uiState: StateFlow<SessionUiState> = observeSession()
        .map { if (it != null) SessionUiState.Authenticated else SessionUiState.Unauthenticated }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SessionUiState.Loading)

    fun logout() {
        viewModelScope.launch { logoutUseCase() }
    }
}