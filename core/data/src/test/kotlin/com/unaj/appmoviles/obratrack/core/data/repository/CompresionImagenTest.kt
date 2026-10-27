package com.unaj.appmoviles.obratrack.core.data.repository

import android.media.ExifInterface
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class CompresionImagenTest {

    @Test
    fun dimensionesDestino_noAgrandaLasFotosChicas() {
        assertEquals(800 to 600, dimensionesDestino(800, 600, 1920))
    }

    @Test
    fun dimensionesDestino_achicaConservandoLaProporcion() {
        assertEquals(1920 to 1440, dimensionesDestino(4000, 3000, 1920))
        assertEquals(1440 to 1920, dimensionesDestino(3000, 4000, 1920))
    }

    @Test
    fun calcularInSampleSize_esPotenciaDe2SinQuedarPorDebajoDelLado() {
        assertEquals(1, calcularInSampleSize(1920, 1080, 1920))
        assertEquals(1, calcularInSampleSize(3000, 2000, 1920))
        assertEquals(2, calcularInSampleSize(4000, 3000, 1920))
        assertEquals(4, calcularInSampleSize(8000, 6000, 1920))
    }

    @Test
    fun transformacionExif_cubreLasOchoOrientaciones() {
        assertEquals(TransformacionExif(0, false), transformacionExif(ExifInterface.ORIENTATION_NORMAL))
        assertEquals(TransformacionExif(90, false), transformacionExif(ExifInterface.ORIENTATION_ROTATE_90))
        assertEquals(TransformacionExif(180, false), transformacionExif(ExifInterface.ORIENTATION_ROTATE_180))
        assertEquals(TransformacionExif(270, false), transformacionExif(ExifInterface.ORIENTATION_ROTATE_270))
        assertEquals(TransformacionExif(0, true), transformacionExif(ExifInterface.ORIENTATION_FLIP_HORIZONTAL))
        assertEquals(TransformacionExif(180, true), transformacionExif(ExifInterface.ORIENTATION_FLIP_VERTICAL))
        assertEquals(TransformacionExif(90, true), transformacionExif(ExifInterface.ORIENTATION_TRANSPOSE))
        assertEquals(TransformacionExif(270, true), transformacionExif(ExifInterface.ORIENTATION_TRANSVERSE))
        assertTrue(transformacionExif(ExifInterface.ORIENTATION_UNDEFINED).esIdentidad)
    }

    @Test
    fun comprimirHastaObjetivo_sePareEnLaPrimeraCalidadQueAlcanza() {
        val probadas = mutableListOf<Int>()
        val salida = comprimirHastaObjetivo(listOf(85, 75, 65, 50), objetivo = 100) { calidad ->
            probadas += calidad
            ByteArray(if (calidad > 70) 200 else 80)
        }

        assertEquals(listOf(85, 75, 65), probadas.take(3))
        assertEquals(3, probadas.size)
        assertEquals(80, salida.size)
    }

    @Test
    fun comprimirHastaObjetivo_siNingunaAlcanzaDevuelveLaMasChica() {
        val salida = comprimirHastaObjetivo(listOf(85, 50), objetivo = 10) { calidad ->
            ByteArray(if (calidad == 85) 300 else 200)
        }

        assertEquals(200, salida.size)
    }

    @Test
    fun prepararFoto_fotoChicaYDerechaSeSubeSinTocar() {
        val original = ByteArray(500_000)

        val r = prepararFoto(original, 1600, 1200, TransformacionExif(0, false)) { error("no debe achicar") }

        assertSame(original, (r as FotoPreparada.Lista).bytes)
    }

    @Test
    fun prepararFoto_fotoGrandeSeAchica() {
        val achicada = ByteArray(900_000)

        val r = prepararFoto(ByteArray(8_000_000), 4000, 3000, TransformacionExif(0, false)) { achicada }

        assertSame(achicada, (r as FotoPreparada.Lista).bytes)
    }

    @Test
    fun prepararFoto_fotoChicaPeroRotadaSeProcesa() {
        val girada = ByteArray(100)

        val r = prepararFoto(ByteArray(500_000), 1600, 1200, TransformacionExif(90, false)) { girada }

        assertArrayEquals(girada, (r as FotoPreparada.Lista).bytes)
    }

    @Test
    fun prepararFoto_siNoBajaDelTopeEsDemasiadoGrande() {
        val r = prepararFoto(ByteArray(9_000_000), 4000, 3000, TransformacionExif(0, false)) {
            ByteArray(PESO_MAXIMO_BYTES + 1)
        }

        assertEquals(FotoPreparada.DemasiadoGrande, r)
    }

    @Test
    fun prepararFoto_noDecodificableSeSubeSoloSiEntraEnElTope() {
        val chico = ByteArray(2_000_000)
        val enorme = ByteArray(PESO_MAXIMO_BYTES + 1)

        assertSame(chico, (prepararFoto(chico, 0, 0, TransformacionExif(0, false)) { null } as FotoPreparada.Lista).bytes)
        assertEquals(FotoPreparada.DemasiadoGrande, prepararFoto(enorme, 0, 0, TransformacionExif(0, false)) { null })
    }
}
