package com.unaj.appmoviles.obratrack.core.data.fotos

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class FotosPendientesStoreTest {

    @get:Rule
    val carpeta = TemporaryFolder()

    private val raiz by lazy { carpeta.newFolder("fotos_pendientes") }
    private val store by lazy { FotosPendientesStore(raiz) }

    @Test
    fun guardarDosVeces_dejaUnSoloArchivoConLaFotoNueva() {
        store.guardar("obra-1", "r1", byteArrayOf(1))
        store.guardar("obra-1", "r1", byteArrayOf(2, 2))

        assertArrayEquals(byteArrayOf(2, 2), store.archivo("obra-1", "r1")!!.readBytes())
        // Sin temporales sueltos.
        assertEquals(listOf("r1.jpg"), File(raiz, "obra-1").list()!!.toList())
    }

    @Test
    fun borrar_soloQuitaEsaFoto() {
        store.guardar("obra-1", "r1", byteArrayOf(1))
        store.guardar("obra-1", "r2", byteArrayOf(2))

        store.borrar("obra-1", "r1")

        assertNull(store.archivo("obra-1", "r1"))
        assertNotNull(store.archivo("obra-1", "r2"))
    }

    @Test
    fun borrarObra_quitaTodasSusFotosYNoLasDeOtras() {
        store.guardar("obra-1", "r1", byteArrayOf(1))
        store.guardar("obra-2", "r2", byteArrayOf(2))

        store.borrarObra("obra-1")

        assertNull(store.archivo("obra-1", "r1"))
        assertNotNull(store.archivo("obra-2", "r2"))
    }

    @Test
    fun archivoInexistente_esNull() {
        assertNull(store.archivo("obra-1", "no-existe"))
    }
}
