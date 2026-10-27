package com.unaj.appmoviles.obratrack.core.data.fotos

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ColaFotosPendientesImplTest {

    @get:Rule
    val carpeta = TemporaryFolder()

    private val planificador = FakePlanificador()
    private val store by lazy { FotosPendientesStore(carpeta.newFolder("fotos_pendientes")) }
    private val cola by lazy { ColaFotosPendientesImpl(store, planificador) }

    @Test
    fun encolar_guardaLaFotoYProgramaLaSubida() = runBlocking {
        cola.encolar("obra-1", "r1", byteArrayOf(1, 2, 3))

        assertArrayEquals(byteArrayOf(1, 2, 3), store.archivo("obra-1", "r1")!!.readBytes())
        assertEquals(listOf(SubidaProgramada("obra-1", "r1")), planificador.programaciones)
        assertEquals(setOf("r1"), cola.pendientes.first().keys)
    }

    @Test
    fun editarConOtraFoto_reemplazaElArchivoYLaSubidaSinDuplicar() = runBlocking {
        cola.encolar("obra-1", "r1", byteArrayOf(1))
        cola.encolar("obra-1", "r1", byteArrayOf(9, 9))

        // Un solo archivo con la foto nueva y una sola subida activa (la segunda reemplazó a la primera).
        assertArrayEquals(byteArrayOf(9, 9), store.archivo("obra-1", "r1")!!.readBytes())
        assertEquals(2, planificador.programaciones.size)
        assertEquals(setOf(SubidaProgramada("obra-1", "r1")), planificador.activas.value)
        assertEquals(1, cola.pendientes.first().size)
    }

    @Test
    fun borrarElRelevamiento_cancelaLaSubidaYBorraElArchivo() = runBlocking {
        cola.encolar("obra-1", "r1", byteArrayOf(1))
        cola.encolar("obra-1", "r2", byteArrayOf(2))

        cola.cancelar("obra-1", "r1")

        assertEquals(listOf("r1"), planificador.canceladas)
        assertNull(store.archivo("obra-1", "r1"))
        // La otra foto sigue en espera.
        assertNotNull(store.archivo("obra-1", "r2"))
        assertEquals(setOf("r2"), cola.pendientes.first().keys)
    }

    @Test
    fun borrarLaObra_cancelaTodasSusSubidasYBorraSusArchivos() = runBlocking {
        cola.encolar("obra-1", "r1", byteArrayOf(1))
        cola.encolar("obra-1", "r2", byteArrayOf(2))
        cola.encolar("obra-2", "r3", byteArrayOf(3))

        cola.cancelarDeObra("obra-1")

        assertEquals(listOf("obra-1"), planificador.obrasCanceladas)
        assertNull(store.archivo("obra-1", "r1"))
        assertNull(store.archivo("obra-1", "r2"))
        assertNotNull(store.archivo("obra-2", "r3"))
        assertEquals(setOf("r3"), cola.pendientes.first().keys)
    }

    @Test
    fun unaSubidaSinArchivo_noApareceComoPendiente() = runBlocking {
        planificador.programar("obra-1", "fantasma")

        assertFalse(cola.pendientes.first().containsKey("fantasma"))
    }
}
