package com.unaj.appmoviles.obratrack.core.data.repository

import com.google.firebase.firestore.FirebaseFirestoreException
import com.unaj.appmoviles.obratrack.core.domain.model.MotivoFalloEscritura
import com.unaj.appmoviles.obratrack.core.domain.repository.FallosEscrituraRepository
import com.unaj.appmoviles.obratrack.core.firebase.FallosEscritura
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

fun Exception.toMotivoFalloEscritura(): MotivoFalloEscritura =
    if (this is FirebaseFirestoreException && code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
        MotivoFalloEscritura.SIN_PERMISO
    } else {
        MotivoFalloEscritura.OTRO
    }

@Singleton
class FallosEscrituraRepositoryImpl @Inject constructor(
    fallosEscritura: FallosEscritura
) : FallosEscrituraRepository {
    override val fallos: Flow<MotivoFalloEscritura> =
        fallosEscritura.fallos.map { it.toMotivoFalloEscritura() }
}
