import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.unaj.appmoviles.features.auth.login.LoginViewModel
import com.unaj.appmoviles.features.auth.register.RegisterViewModel
import com.unaj.appmoviles.features.obras.form.ObraFormViewModel
import com.unaj.appmoviles.features.obras.list.ObrasListViewModel
import com.unaj.appmoviles.obratrack.core.data.repository.AuthRepositoryImpl
import com.unaj.appmoviles.obratrack.core.data.repository.ObraRepositoryImpl
import com.unaj.appmoviles.obratrack.core.domain.repository.AuthRepository
import com.unaj.appmoviles.obratrack.core.domain.repository.ObraRepository
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.CreateObraUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.auth.LoginUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.auth.LogoutUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.ObserveObrasUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.auth.ObserveSessionUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.auth.RegisterUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.UpdateObraUseCase
import com.unaj.appmoviles.obratrack.core.firebase.FirebaseAuthDataSourceImpl
import com.unaj.appmoviles.obratrack.core.firebase.FirestoreObraDataSourceImpl
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppContainer {
    val authRepository: AuthRepository =
        AuthRepositoryImpl(FirebaseAuthDataSourceImpl(FirebaseAuth.getInstance()))

    val obraRepository: ObraRepository =
        ObraRepositoryImpl(FirestoreObraDataSourceImpl(FirebaseFirestore.getInstance()))

    private val loginUseCase = LoginUseCase(authRepository)
    private val registerUseCase = RegisterUseCase(authRepository)
    private val logoutUseCase = LogoutUseCase(authRepository)
    private val observeSessionUseCase = ObserveSessionUseCase(authRepository)
    private val observeObrasUseCase = ObserveObrasUseCase(obraRepository)
    private val createObraUseCase = CreateObraUseCase(obraRepository)
    private val updateObraUseCase = UpdateObraUseCase(obraRepository)

    val loginViewModelFactory = viewModelFactory { initializer { LoginViewModel(loginUseCase) } }
    val registerViewModelFactory = viewModelFactory { initializer {
        RegisterViewModel(
            registerUseCase
        )
    } }
    val sessionViewModelFactory = viewModelFactory {
        initializer { SessionViewModel(observeSessionUseCase, logoutUseCase) }
    }
    val obrasListViewModelFactory = viewModelFactory {
        initializer { ObrasListViewModel(observeSessionUseCase, observeObrasUseCase) }
    }

    fun obraFormViewModelFactory(obraId: String?) = viewModelFactory {
        initializer {
            ObraFormViewModel(
                observeSessionUseCase,
                createObraUseCase,
                updateObraUseCase,
                obraId
            )
        }
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