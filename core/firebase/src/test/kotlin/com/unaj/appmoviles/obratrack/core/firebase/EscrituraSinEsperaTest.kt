package com.unaj.appmoviles.obratrack.core.firebase

import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class EscrituraSinEsperaTest {

    @Test
    fun escrituraRechazada_sePublicaEnFallos() = runBlocking {
        val fallos = FallosEscritura()
        val error = IllegalStateException("rechazada")
        val recibido = async(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) {
            fallos.fallos.first()
        }

        Tasks.forException<Void>(error).sinEsperar(fallos)

        assertSame(error, recibido.await())
    }

    @Test
    fun escrituraCorrecta_noPublicaNada() = runBlocking {
        val fallos = FallosEscritura()
        val recibidos = mutableListOf<Exception>()
        val job = launch(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) {
            fallos.fallos.toList(recibidos)
        }

        Tasks.forResult<Void>(null).sinEsperar(fallos)
        job.cancel()

        assertEquals(emptyList<Exception>(), recibidos)
    }
}
