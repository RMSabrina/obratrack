package com.unaj.appmoviles.features.obras.tareas

import com.unaj.appmoviles.obratrack.core.domain.error.AppResult
import com.unaj.appmoviles.obratrack.core.domain.error.TareaError
import com.unaj.appmoviles.obratrack.core.domain.model.Credentials
import com.unaj.appmoviles.obratrack.core.domain.model.EstadoObra
import com.unaj.appmoviles.obratrack.core.domain.model.Obra
import com.unaj.appmoviles.obratrack.core.domain.model.ObraDraft
import com.unaj.appmoviles.obratrack.core.domain.model.Relevamiento
import com.unaj.appmoviles.obratrack.core.domain.model.RelevamientoDraft
import com.unaj.appmoviles.obratrack.core.domain.model.RelevamientosVinculados
import com.unaj.appmoviles.obratrack.core.domain.model.Tarea
import com.unaj.appmoviles.obratrack.core.domain.model.TareaDraft
import com.unaj.appmoviles.obratrack.core.domain.model.User
import com.unaj.appmoviles.obratrack.core.domain.repository.AuthRepository
import com.unaj.appmoviles.obratrack.core.domain.repository.ObraRepository
import com.unaj.appmoviles.obratrack.core.domain.repository.RelevamientoRepository
import com.unaj.appmoviles.obratrack.core.domain.repository.TareaRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

const val OBRA_ID = "obra-1"
val DUENO = User(id = "dueno", email = "dueno@test.com", name = "Dueño")
val OTRO_USUARIO = User(id = "otro", email = "otro@test.com", name = "Otro")

fun obraDe(userId: String, publica: Boolean = false) = Obra(
    id = OBRA_ID,
    userId = userId,
    nombre = "Casa",
    cliente = "Cliente",
    direccion = "Calle 123",
    publica = publica,
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

/** Igual que la implementación real: observeOne devuelve null si la obra no es del usuario. */
class FakeObraRepository(obra: Obra?) : ObraRepository {
    val obra = MutableStateFlow(obra)

    /** Si no es null, observe y observeOne fallan con esta excepción (como Firestore). */
    var observeError: Exception? = null

    override fun observeOne(userId: String, obraId: String): Flow<Obra?> {
        val error = observeError
        if (error != null) return flow { throw error }
        return this.obra.map { it?.takeIf { o -> o.id == obraId && o.userId == userId } }
    }

    /** Si no es null, observePublicas y observePublica fallan con esta excepción (como Firestore). */
    var observePublicasError: Exception? = null

    override fun observe(userId: String): Flow<List<Obra>> {
        val error = observeError
        if (error != null) return flow { throw error }
        return this.obra.map { listOfNotNull(it?.takeIf { o -> o.userId == userId }) }
    }
    override fun observePublicas(): Flow<List<Obra>> {
        val error = observePublicasError
        if (error != null) return flow { throw error }
        return this.obra.map { listOfNotNull(it?.takeIf { o -> o.publica }) }
    }
    override fun observePublica(obraId: String): Flow<Obra?> {
        val error = observePublicasError
        if (error != null) return flow { throw error }
        return this.obra.map { it?.takeIf { o -> o.id == obraId && o.publica } }
    }
    override suspend fun create(userId: String, draft: ObraDraft): AppResult<Obra> = TODO()
    override suspend fun update(userId: String, obraId: String, draft: ObraDraft): AppResult<Obra> = TODO()
    override suspend fun existsAndOwnedByCurrentUser(obraId: String): AppResult<Boolean> = TODO()
    override suspend fun delete(obraId: String): AppResult<Unit> = TODO()
    override suspend fun deleteAllSuscriptoresByObra(obraId: String): AppResult<Unit> = TODO()
}

class FakeRelevamientoRepository(relevamientos: List<Relevamiento> = emptyList()) : RelevamientoRepository {
    val relevamientos = MutableStateFlow(relevamientos)

    /** Si no es null, observe y observeByObra fallan con esta excepción (como Firestore). */
    var observeError: Exception? = null

    override fun observe(userId: String, obraId: String): Flow<List<Relevamiento>> =
        observeByObra(obraId)

    /** Si no es null, observeByTarea falla con esta excepción. */
    var observeByTareaError: Exception? = null
    var observeByTareaCalls = 0
    val created = mutableListOf<RelevamientoDraft>()
    val updated = mutableListOf<Pair<String, RelevamientoDraft>>()

    override fun observeAll(userId: String): Flow<List<Relevamiento>> = TODO()
    override fun observeByObra(obraId: String): Flow<List<Relevamiento>> {
        val error = observeError
        if (error != null) return flow { throw error }
        return relevamientos.map { lista -> lista.filter { it.obraId == obraId } }
    }
    override fun observeByTarea(obraId: String, tareaId: String): Flow<List<Relevamiento>> {
        observeByTareaCalls++
        val error = observeByTareaError
        if (error != null) return flow { throw error }
        return relevamientos.map { lista -> lista.filter { it.obraId == obraId && it.tareaId == tareaId } }
    }
    override suspend fun create(
        userId: String,
        obraId: String,
        draft: RelevamientoDraft,
        photo: ByteArray?
    ): AppResult<Relevamiento> {
        created += draft
        return AppResult.Success(draft.toRelevamiento("nuevo", obraId, userId))
    }
    override suspend fun update(
        userId: String,
        obraId: String,
        relevamientoId: String,
        draft: RelevamientoDraft,
        photo: ByteArray?
    ): AppResult<Relevamiento> {
        updated += relevamientoId to draft
        return AppResult.Success(draft.toRelevamiento(relevamientoId, obraId, userId))
    }
    override suspend fun delete(obraId: String, relevamientoId: String): AppResult<Unit> = TODO()
    override suspend fun deleteAllByObra(obraId: String): AppResult<Unit> = TODO()
}

class FakeTareaRepository(tareas: List<Tarea> = emptyList()) : TareaRepository {
    val tareas = MutableStateFlow(tareas)

    /** Si no es null, observeByObra falla con esta excepción (como un error de Firestore). */
    var observeError: Exception? = null
    /** Si no es null, create y update devuelven este error. */
    var writeError: TareaError? = null
    /** Si no es null, delete devuelve este error. */
    var deleteError: TareaError? = null
    /** Simula el borrado offline: la tarea sale del caché aunque delete devuelva error. */
    var deleteRemovesOnError = false

    var observeByObraCalls = 0
    val created = mutableListOf<Pair<String, TareaDraft>>()
    val updated = mutableListOf<Triple<String, String, TareaDraft>>()
    val deleted = mutableListOf<Pair<String, String>>()

    override fun observeByObra(obraId: String): Flow<List<Tarea>> {
        observeByObraCalls++
        val error = observeError
        if (error != null) return flow { throw error }
        return tareas.map { lista -> lista.filter { it.obraId == obraId } }
    }

    override fun observeOne(obraId: String, tareaId: String): Flow<Tarea?> =
        tareas.map { lista -> lista.find { it.obraId == obraId && it.id == tareaId } }

    override suspend fun create(userId: String, obraId: String, draft: TareaDraft): AppResult<Tarea> {
        writeError?.let { return AppResult.Error(it) }
        created += userId to draft
        return AppResult.Success(draft.toTarea("nueva", obraId, userId))
    }

    override suspend fun update(
        userId: String,
        obraId: String,
        tareaId: String,
        draft: TareaDraft
    ): AppResult<Tarea> {
        writeError?.let { return AppResult.Error(it) }
        updated += Triple(userId, tareaId, draft)
        val actualizada = draft.toTarea(tareaId, obraId, userId)
        tareas.value = tareas.value.map { if (it.obraId == obraId && it.id == tareaId) actualizada else it }
        return AppResult.Success(actualizada)
    }

    /** Opción elegida para los vinculados en cada borrado exitoso. */
    val deletedVinculados = mutableListOf<RelevamientosVinculados>()

    override suspend fun delete(
        obraId: String,
        tareaId: String,
        vinculados: RelevamientosVinculados
    ): AppResult<Unit> {
        val error = deleteError
        if (error == null || deleteRemovesOnError) {
            tareas.value = tareas.value.filterNot { it.obraId == obraId && it.id == tareaId }
        }
        if (error != null) {
            // Como en Firestore, el error (p. ej. timeout) llega después del cambio en el caché.
            delay(1_000)
            return AppResult.Error(error)
        }
        deleted += obraId to tareaId
        deletedVinculados += vinculados
        return AppResult.Success(Unit)
    }
    override suspend fun deleteAllByObra(obraId: String): AppResult<Unit> = TODO()
}

private fun TareaDraft.toTarea(id: String, obraId: String, userId: String) = Tarea(
    id = id,
    obraId = obraId,
    userId = userId,
    titulo = titulo,
    descripcion = descripcion,
    categoria = categoria,
    fechaInicio = fechaInicio,
    fechaFin = fechaFin,
    estado = estado
)

private fun RelevamientoDraft.toRelevamiento(id: String, obraId: String, userId: String) = Relevamiento(
    id = id,
    obraId = obraId,
    userId = userId,
    fecha = fecha,
    nota = nota,
    tareaId = tareaId
)
