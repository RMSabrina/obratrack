package com.unaj.appmoviles.features.obras.export

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.concurrent.TimeUnit

class CacheTemporalTest {

    @get:Rule
    val carpeta = TemporaryFolder()

    private val ahora = 1_800_000_000_000L

    private fun archivo(nombre: String, horasDeAntiguedad: Long): File =
        carpeta.newFile(nombre).also { it.setLastModified(ahora - TimeUnit.HOURS.toMillis(horasDeAntiguedad)) }

    @Test
    fun pdfs_soloSeleccionaLosPropiosYVencidos() {
        val pdfViejo = archivo("informe_obra_Casa.pdf", horasDeAntiguedad = 30)
        val actaVieja = archivo("acta_visita_r1.pdf", horasDeAntiguedad = 48)
        archivo("informe_obra_Reciente.pdf", horasDeAntiguedad = 2)
        archivo("otra_cosa.pdf", horasDeAntiguedad = 100)
        archivo("relevamiento-123.jpg", horasDeAntiguedad = 100)

        val vencidos = pdfsTemporalesVencidos(carpeta.root, ahora)

        assertEquals(setOf(pdfViejo, actaVieja), vencidos.toSet())
    }

    @Test
    fun fotos_noTocaLasQueUnFormularioAbiertoUsa() {
        val enUso = archivo("relevamiento-1.jpg", horasDeAntiguedad = 72)
        val sinUso = archivo("relevamiento-2.jpg", horasDeAntiguedad = 72)
        archivo("relevamiento-3.jpg", horasDeAntiguedad = 1)

        val vencidas = fotosTemporalesVencidas(carpeta.root, ahora, enUso = setOf(enUso.absolutePath))

        assertEquals(listOf(sinUso), vencidas)
    }

    @Test
    fun limpiarFotos_borraLasVencidasSinUsoYDejaElResto() {
        val enUso = archivo("relevamiento-1.jpg", horasDeAntiguedad = 72)
        val sinUso = archivo("relevamiento-2.jpg", horasDeAntiguedad = 72)
        val reciente = archivo("relevamiento-3.jpg", horasDeAntiguedad = 1)
        val pdf = archivo("informe_obra_X.pdf", horasDeAntiguedad = 72)

        limpiarFotosTemporales(carpeta.root, setOf(enUso.absolutePath), ahora)

        assertTrue(enUso.exists())
        assertFalse(sinUso.exists())
        assertTrue(reciente.exists())
        assertTrue(pdf.exists())
    }

    @Test
    fun limpiarPdfs_borraLosVencidosYDejaLasFotos() {
        val viejo = archivo("informe_obra_Casa.pdf", horasDeAntiguedad = 30)
        val reciente = archivo("acta_visita_r2.pdf", horasDeAntiguedad = 1)
        val foto = archivo("relevamiento-9.jpg", horasDeAntiguedad = 500)
        val ajeno = archivo("datos.db", horasDeAntiguedad = 500)

        limpiarPdfsTemporales(carpeta.root, ahora)

        assertFalse(viejo.exists())
        assertTrue(reciente.exists())
        assertTrue(foto.exists())
        assertTrue(ajeno.exists())
    }

    @Test
    fun fotosEnUso_registraYLiberaConOSinPrefijoFile() {
        FotosEnUso.registrar("file:///cache/relevamiento-7.jpg")
        assertTrue("/cache/relevamiento-7.jpg" in FotosEnUso.rutas())

        FotosEnUso.liberar("/cache/relevamiento-7.jpg")
        assertFalse("/cache/relevamiento-7.jpg" in FotosEnUso.rutas())
    }

    @Test
    fun noTocaSubcarpetasNiFallaConCarpetaInexistente() {
        val sub = carpeta.newFolder("informe_obra_dir.pdf")
        sub.setLastModified(ahora - TimeUnit.DAYS.toMillis(10))

        limpiarPdfsTemporales(carpeta.root, ahora)
        limpiarPdfsTemporales(File(carpeta.root, "no-existe"), ahora)

        assertTrue(sub.exists())
    }
}
