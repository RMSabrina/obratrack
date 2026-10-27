package com.unaj.appmoviles.obratrack.core.data.repository

import com.unaj.appmoviles.obratrack.core.domain.model.MotivoFalloEscritura
import com.unaj.appmoviles.obratrack.core.firebase.FallosEscritura
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

// FirebaseFirestoreException no se puede construir en un test JVM: el caso PERMISSION_DENIED
// (→ SIN_PERMISO) queda para una prueba instrumentada.
class FallosEscrituraRepositoryImplTest {

    @Test
    fun errorQueNoEsDeFirestore_esOtro() {
        assertEquals(MotivoFalloEscritura.OTRO, IllegalStateException().toMotivoFalloEscritura())
    }

    @Test
    fun elRepositorioTraduceLosFallosReportados() = runBlocking {
        val origen = FallosEscritura()
        val repo = FallosEscrituraRepositoryImpl(origen)
        val motivo = async(start = CoroutineStart.UNDISPATCHED) { repo.fallos.first() }

        origen.reportar(IllegalStateException("rechazada"))

        assertEquals(MotivoFalloEscritura.OTRO, motivo.await())
    }
}
