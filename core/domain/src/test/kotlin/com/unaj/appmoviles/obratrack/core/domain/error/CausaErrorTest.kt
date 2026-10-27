package com.unaj.appmoviles.obratrack.core.domain.error

import org.junit.Assert.assertEquals
import org.junit.Test

class CausaErrorTest {

    @Test
    fun clasificaLosErroresDeLecturaPorCausa() {
        assertEquals(CausaError.SIN_CONEXION, ObraError.Red.causa())
        assertEquals(CausaError.SIN_CONEXION, RelevamientoError.Red.causa())
        assertEquals(CausaError.SIN_CONEXION, TareaError.Red.causa())
        assertEquals(CausaError.SIN_PERMISO, ObraError.NoAutorizada.causa())
        assertEquals(CausaError.SIN_PERMISO, TareaError.NoAutorizada.causa())
        assertEquals(CausaError.NO_ENCONTRADO, RelevamientoError.NoEncontrado.causa())
        assertEquals(CausaError.TARDO_DEMASIADO, ObraError.Timeout.causa())
        assertEquals(CausaError.OTRA, ObraError.Desconocido.causa())
        assertEquals(CausaError.OTRA, ObraError.NombreVacio.causa())
    }

    @Test
    fun laFallaDeLecturaClasificadaConservaSuError() {
        val falla = LecturaFallidaException(TareaError.Red, IllegalStateException("x"))

        assertEquals(TareaError.Red, falla.aErrorDeLectura())
    }

    @Test
    fun unaFallaSinClasificarEsDesconocida() {
        assertEquals(ObraError.Desconocido, IllegalStateException("x").aErrorDeLectura())
        assertEquals(
            TareaError.Desconocido,
            IllegalStateException("x").aErrorDeLectura(TareaError.Desconocido)
        )
    }
}
