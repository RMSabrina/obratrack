import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.unaj.appmoviles.features.auth.login.LoginViewModel
import com.unaj.appmoviles.features.auth.register.RegisterViewModel
import com.unaj.appmoviles.features.obras.detail.ObraDetailViewModel
import com.unaj.appmoviles.features.obras.form.ObraFormViewModel
import com.unaj.appmoviles.features.obras.list.ObrasListViewModel
import com.unaj.appmoviles.features.obras.relevamiento.RelevamientoFormViewModel
import com.unaj.appmoviles.features.home.HomeViewModel
import com.unaj.appmoviles.obratrack.core.data.repository.AuthRepositoryImpl
import com.unaj.appmoviles.obratrack.core.data.repository.ObraRepositoryImpl
import com.unaj.appmoviles.obratrack.core.data.repository.RelevamientoRepositoryImpl
import com.unaj.appmoviles.obratrack.core.domain.repository.AuthRepository
import com.unaj.appmoviles.obratrack.core.domain.repository.ObraRepository
import com.unaj.appmoviles.obratrack.core.domain.repository.RelevamientoRepository
import com.unaj.appmoviles.obratrack.core.domain.usecase.auth.LoginUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.auth.LogoutUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.auth.ObserveSessionUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.auth.RegisterUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.auth.ResendVerificationUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.CreateObraUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.ObserveObraUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.ObserveObrasUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.UpdateObraUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.relevamientos.ObserveAllRelevamientosUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.relevamientos.CreateRelevamientoUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.relevamientos.ObserveRelevamientosUseCase
import com.unaj.appmoviles.obratrack.core.domain.usecase.relevamientos.UpdateRelevamientoUseCase
import com.unaj.appmoviles.obratrack.core.firebase.FirebaseAuthDataSourceImpl
import com.unaj.appmoviles.obratrack.core.firebase.FirestoreObraDataSourceImpl
import com.unaj.appmoviles.obratrack.core.firebase.FirestoreRelevamientoDataSourceImpl
import com.unaj.appmoviles.obratrack.core.data.repository.CloudinaryImageStorageImpl
import com.unaj.appmoviles.obratrack.BuildConfig
import com.cloudinary.Cloudinary
import com.cloudinary.utils.ObjectUtils
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppContainer {
    
    private val cloudinaryConfig = ObjectUtils.asMap(
        "cloud_name", BuildConfig.CLOUDINARY_CLOUD_NAME
    )
    
    val imageStorageRepository = CloudinaryImageStorageImpl(
        Cloudinary(cloudinaryConfig),
        uploadPreset = BuildConfig.CLOUDINARY_UPLOAD_PRESET
    )

    val authRepository: AuthRepository =
        AuthRepositoryImpl(FirebaseAuthDataSourceImpl(FirebaseAuth.getInstance()))

    val obraRepository: ObraRepository =
        ObraRepositoryImpl(FirestoreObraDataSourceImpl(FirebaseFirestore.getInstance()))

    val relevamientoRepository: RelevamientoRepository =
        RelevamientoRepositoryImpl(
            FirestoreRelevamientoDataSourceImpl(FirebaseFirestore.getInstance()),
            imageStorageRepository
        )

    private val loginUseCase = LoginUseCase(authRepository)
    private val registerUseCase = RegisterUseCase(authRepository)
    private val logoutUseCase = LogoutUseCase(authRepository)
    private val observeSessionUseCase = ObserveSessionUseCase(authRepository)
    private val observeObrasUseCase = ObserveObrasUseCase(obraRepository)
    private val createObraUseCase = CreateObraUseCase(obraRepository)
    private val updateObraUseCase = UpdateObraUseCase(obraRepository)
    private val observeObraUseCase = ObserveObraUseCase(obraRepository)
    private val observeRelevamientosUseCase = ObserveRelevamientosUseCase(relevamientoRepository)
    private val observeAllRelevamientosUseCase = ObserveAllRelevamientosUseCase(relevamientoRepository)
    private val createRelevamientoUseCase = CreateRelevamientoUseCase(relevamientoRepository)
    private val updateRelevamientoUseCase = UpdateRelevamientoUseCase(relevamientoRepository)

    val loginViewModelFactory = viewModelFactory { initializer { LoginViewModel(loginUseCase) } }
    private val resendVerificationUseCase = ResendVerificationUseCase(authRepository)
    val registerViewModelFactory = viewModelFactory {
        initializer { RegisterViewModel(registerUseCase, resendVerificationUseCase) }
    }
    val sessionViewModelFactory = viewModelFactory {
        initializer { SessionViewModel(observeSessionUseCase, logoutUseCase) }
    }
    val obrasListViewModelFactory = viewModelFactory {
        initializer { ObrasListViewModel(observeSessionUseCase, observeObrasUseCase) }
    }

    val homeViewModelFactory = viewModelFactory {
        initializer { HomeViewModel(observeSessionUseCase, observeObrasUseCase, observeAllRelevamientosUseCase) }
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

    fun obraDetailViewModelFactory(obraId: String) = viewModelFactory {
        initializer {
            ObraDetailViewModel(
                obraId,
                observeSessionUseCase,
                observeObraUseCase,
                observeRelevamientosUseCase
            )
        }
    }

    fun relevamientoFormViewModelFactory(obraId: String, relevamientoId: String?) = viewModelFactory {
        initializer {
            RelevamientoFormViewModel(
                observeSessionUseCase,
                createRelevamientoUseCase,
                updateRelevamientoUseCase,
                obraId,
                relevamientoId
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