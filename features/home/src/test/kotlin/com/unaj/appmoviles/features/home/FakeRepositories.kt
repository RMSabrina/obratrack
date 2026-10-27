package com.unaj.appmoviles.features.home

import com.unaj.appmoviles.obratrack.core.domain.error.AppResult
import com.unaj.appmoviles.obratrack.core.domain.model.Credentials
import com.unaj.appmoviles.obratrack.core.domain.model.EstadoObra
import com.unaj.appmoviles.obratrack.core.domain.model.Obra
import com.unaj.appmoviles.obratrack.core.domain.model.ObraDraft
import com.unaj.appmoviles.obratrack.core.domain.model.Relevamiento
import com.unaj.appmoviles.obratrack.core.domain.model.RelevamientoDraft
import com.unaj.appmoviles.obratrack.core.domain.model.User
import com.unaj.appmoviles.obratrack.core.domain.repository.AuthRepository
import com.unaj.appmoviles.obratrack.core.domain.repository.ObraRepository
import com.unaj.appmoviles.obratrack.core.domain.repository.RelevamientoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

val USUARIO = User(id = "uid", email = "uid@test.com", name = "Ana")

fun obraDe(id: String = "obra-1", userId: String = USUARIO.id) = Obra(
    id = id,
    userId = userId,
    nombre = "Casa $id",
    cliente = "Cliente",
    direccion = "Calle 123",
    publica = false,
    estado = EstadoObra.ACTIVA,
    fechaInicio = 0L,
    ultimaVisita = null
)

class FakeAuthRepository(user: User?) : AuthRepository {
    override val session = MutableStateFlow(user)
    override suspend fun login(credentials: Credentials): AppResult<User> = TODO()
    override suspend fun loginWithGoogle(idToken: String): AppResult<User> = TODO()
    override suspend fun register(name: String, credentials: Credentials): AppResult<User> = TODO()
    override suspend fun logout() = TODO()
    override suspend fun resendVerificationEmail(email: String, password: String): AppResult<Unit> = TODO()
    override suspend fun updateProfileName(newName: String): AppResult<User> = TODO()
    override suspend fun updatePassword(newPassword: String): AppResult<Unit> = TODO()
    override suspend fun deleteAccount(): AppResult<Unit> = TODO()
    override suspend fun reauthenticate(password: String): AppResult<Unit> = TODO()
}

class FakeObraRepository(obras: List<Obra> = emptyList()) : ObraRepository {
    val obras = MutableStateFlow(obras)

    /** Si no es null, observe falla con esta excepción (como Firestore). */
    var observeError: Exception? = null

    override fun observe(userId: String): Flow<List<Obra>> {
        val error = observeError
        if (error != null) return flow { throw error }
        return obras.map { lista -> lista.filter { it.userId == userId } }
    }

    override fun observeOne(userId: String, obraId: String): Flow<Obra?> = TODO()
    override fun observePublicas(): Flow<List<Obra>> = TODO()
    override fun observePublica(obraId: String): Flow<Obra?> = TODO()
    override suspend fun create(userId: String, draft: ObraDraft): AppResult<Obra> = TODO()
    override suspend fun update(userId: String, obraId: String, draft: ObraDraft): AppResult<Obra> = TODO()
    override suspend fun existsAndOwnedByCurrentUser(obraId: String): AppResult<Boolean> = TODO()
    override suspend fun delete(obraId: String): AppResult<Unit> = TODO()
    override suspend fun deleteAllSuscriptoresByObra(obraId: String): AppResult<Unit> = TODO()
}

class FakeRelevamientoRepository(relevamientos: List<Relevamiento> = emptyList()) : RelevamientoRepository {
    val relevamientos = MutableStateFlow(relevamientos)

    /** Si no es null, observeAll falla con esta excepción (como Firestore). */
    var observeAllError: Exception? = null

    override fun observeAll(userId: String): Flow<List<Relevamiento>> {
        val error = observeAllError
        if (error != null) return flow { throw error }
        return relevamientos.map { lista -> lista.filter { it.userId == userId } }
    }

    override fun observe(userId: String, obraId: String): Flow<List<Relevamiento>> = TODO()
    override fun observeByObra(obraId: String): Flow<List<Relevamiento>> = TODO()
    override fun observeByTarea(obraId: String, tareaId: String): Flow<List<Relevamiento>> = TODO()
    override suspend fun create(
        userId: String,
        obraId: String,
        draft: RelevamientoDraft,
        photo: ByteArray?
    ): AppResult<Relevamiento> = TODO()
    override suspend fun update(
        userId: String,
        obraId: String,
        relevamientoId: String,
        draft: RelevamientoDraft,
        photo: ByteArray?
    ): AppResult<Relevamiento> = TODO()
    override suspend fun delete(obraId: String, relevamientoId: String): AppResult<Unit> = TODO()
    override suspend fun deleteAllByObra(obraId: String): AppResult<Unit> = TODO()
}
