package com.unaj.appmoviles.obratrack.core.data.fotos

import com.unaj.appmoviles.obratrack.core.domain.error.AppResult
import com.unaj.appmoviles.obratrack.core.domain.error.RelevamientoError
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SubidorFotosPendientesTest {

    @get:Rule
    val carpeta = TemporaryFolder()

    private val imagenes = FakeImagenes()
    private val relevamientos = FakeFotoRelevamientos()
    private val store by lazy { FotosPendientesStore(carpeta.newFolder("fotos_pendientes")) }
    private val subidor by lazy { SubidorFotosPendientes(store, imagenes, relevamientos) }

    private fun conFoto() = store.guardar("obra-1", "r1", byteArrayOf(1, 2, 3))

    @Test
    fun conRed_subeAsignaLaUrlYBorraElArchivo() = runBlocking {
        conFoto()

        val resultado = subidor.subir("obra-1", "r1", intento = 0)

        assertEquals(ResultadoSubida.LISTO, resultado)
        assertEquals(listOf(Triple("obra-1", "r1", "https://img/nueva")), relevamientos.asignadas)
        assertNull(store.archivo("obra-1", "r1"))
    }

    @Test
    fun documentoInexistente_noSubeNadaYBorraElArchivo() = runBlocking {
        conFoto()
        relevamientos.existe = AppResult.Success(false)

        val resultado = subidor.subir("obra-1", "r1", intento = 0)

        assertEquals(ResultadoSubida.DESCARTADA, resultado)
        assertEquals(0, imagenes.llamadas)
        assertEquals(emptyList<Triple<String, String, String>>(), relevamientos.asignadas)
        assertNull(store.archivo("obra-1", "r1"))
    }

    @Test
    fun elDocumentoSeBorroDuranteLaSubida_noQuedaNadaPendiente() = runBlocking {
        conFoto()
        relevamientos.asignar = AppResult.Success(false)

        val resultado = subidor.subir("obra-1", "r1", intento = 0)

        assertEquals(ResultadoSubida.DESCARTADA, resultado)
        assertNull(store.archivo("obra-1", "r1"))
    }

    @Test
    fun sinRed_reintentaYConservaLaFoto() = runBlocking {
        conFoto()
        imagenes.resultado = SIN_RED

        // Aunque ya haya muchos intentos: sin red no se rinde.
        val resultado = subidor.subir("obra-1", "r1", intento = 20)

        assertEquals(ResultadoSubida.REINTENTAR, resultado)
        assertNotNull(store.archivo("obra-1", "r1"))
        assertEquals(emptyList<Triple<String, String, String>>(), relevamientos.asignadas)
    }

    @Test
    fun fotoDemasiadoGrande_seDescartaSinReintentar() = runBlocking {
        conFoto()
        imagenes.resultado = AppResult.Error(RelevamientoError.ImagenMuyGrande)

        val resultado = subidor.subir("obra-1", "r1", intento = 0)

        assertEquals(ResultadoSubida.DESCARTADA, resultado)
        assertNull(store.archivo("obra-1", "r1"))
    }

    @Test
    fun errorDesconocido_reintentaHastaAgotarLosIntentos() = runBlocking {
        conFoto()
        imagenes.resultado = AppResult.Error(RelevamientoError.Desconocido)

        assertEquals(ResultadoSubida.REINTENTAR, subidor.subir("obra-1", "r1", intento = 0))
        assertNotNull(store.archivo("obra-1", "r1"))

        val ultimo = subidor.subir("obra-1", "r1", intento = SubidorFotosPendientes.MAX_INTENTOS - 1)

        assertEquals(ResultadoSubida.DESCARTADA, ultimo)
        assertNull(store.archivo("obra-1", "r1"))
    }

    @Test
    fun sinArchivo_nadaQueHacer() = runBlocking {
        assertEquals(ResultadoSubida.DESCARTADA, subidor.subir("obra-1", "r1", intento = 0))
        assertEquals(0, imagenes.llamadas)
    }

    @Test
    fun cadaSubidaUsaUnNombreDistinto() = runBlocking {
        conFoto()
        subidor.subir("obra-1", "r1", intento = 0)
        conFoto()
        subidor.subir("obra-1", "r1", intento = 0)

        // Con el mismo nombre, Cloudinary devolvería la imagen vieja en vez de guardar la nueva.
        assertNotEquals(imagenes.nombres[0], imagenes.nombres[1])
    }
}
