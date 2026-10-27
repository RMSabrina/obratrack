package com.unaj.appmoviles.obratrack.core.data.repository

import com.unaj.appmoviles.obratrack.core.domain.error.LecturaFallidaException
import com.unaj.appmoviles.obratrack.core.domain.error.ObraError
import com.unaj.appmoviles.obratrack.core.domain.error.RelevamientoError
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException

class LecturaErroresTest {

    @Test
    fun sinFalla_emiteLosValoresTalCual() = runBlocking {
        val valores = flow { emit(1); emit(2) }.conErrorDeLectura { ObraError.Desconocido }.toList()

        assertEquals(listOf(1, 2), valores)
    }

    @Test
    fun laFallaSeConvierteEnLecturaFallidaConElErrorClasificado() = runBlocking {
        val original = IOException("sin red")
        val origen = flow<Int> { emit(1); throw original }

        val recibido = mutableListOf<Int>()
        val falla = try {
            origen.conErrorDeLectura { ObraError.Red }.toList(recibido)
            null
        } catch (e: LecturaFallidaException) {
            e
        }

        assertEquals(listOf(1), recibido)
        assertEquals(ObraError.Red, falla?.error)
        assertSame(original, falla?.cause)
    }

    @Test
    fun laSubidaDistingueSinConexionTiempoYOtro() {
        assertEquals(RelevamientoError.Timeout, SocketTimeoutException().toUploadError())
        assertEquals(RelevamientoError.Red, IOException().toUploadError())
        assertEquals(RelevamientoError.Desconocido, IllegalStateException().toUploadError())
    }
}
