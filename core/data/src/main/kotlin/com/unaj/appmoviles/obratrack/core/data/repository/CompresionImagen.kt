package com.unaj.appmoviles.obratrack.core.data.repository

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.util.Log
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

private const val TAG = "CompresionImagen"

/** Lado más largo de la foto que se sube; alcanza para ver el detalle de una obra y pesa poco. */
internal const val LADO_MAXIMO_PX = 1920

/** Peso al que se intenta llegar (bajando la calidad de a poco). */
internal const val PESO_OBJETIVO_BYTES = 1_500_000

/** Tope de lo que se sube aunque no se haya podido llegar al objetivo. Cloudinary admite más. */
internal const val PESO_MAXIMO_BYTES = 5 * 1024 * 1024

internal val CALIDADES_JPEG = listOf(85, 75, 65, 50, 40)

/** Resultado de intentar achicar una foto. */
internal sealed interface FotoPreparada {
    class Lista(val bytes: ByteArray) : FotoPreparada

    /** No se pudo dejar por debajo del tope: no se sube. */
    data object DemasiadoGrande : FotoPreparada
}

/** Tamaño final con el lado más largo en [ladoMaximo] como mucho, sin agrandar y sin deformar. */
internal fun dimensionesDestino(ancho: Int, alto: Int, ladoMaximo: Int): Pair<Int, Int> {
    val mayor = maxOf(ancho, alto)
    if (mayor <= ladoMaximo) return ancho to alto
    val escala = ladoMaximo.toDouble() / mayor
    return maxOf(1, Math.round(ancho * escala).toInt()) to maxOf(1, Math.round(alto * escala).toInt())
}

/** Potencia de 2 para decodificar sin cargar la foto entera, sin quedar por debajo del lado pedido. */
internal fun calcularInSampleSize(ancho: Int, alto: Int, ladoMaximo: Int): Int {
    var muestra = 1
    var mayor = maxOf(ancho, alto)
    while (mayor / 2 >= ladoMaximo) {
        mayor /= 2
        muestra *= 2
    }
    return muestra
}

/** Grados a girar (horario) y si hay que espejar antes, según la orientación EXIF. */
internal data class TransformacionExif(val grados: Int, val espejar: Boolean) {
    val esIdentidad: Boolean get() = grados == 0 && !espejar
}

internal fun transformacionExif(orientacion: Int): TransformacionExif = when (orientacion) {
    ExifInterface.ORIENTATION_ROTATE_90 -> TransformacionExif(90, false)
    ExifInterface.ORIENTATION_ROTATE_180 -> TransformacionExif(180, false)
    ExifInterface.ORIENTATION_ROTATE_270 -> TransformacionExif(270, false)
    ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> TransformacionExif(0, true)
    ExifInterface.ORIENTATION_FLIP_VERTICAL -> TransformacionExif(180, true)
    ExifInterface.ORIENTATION_TRANSPOSE -> TransformacionExif(90, true)
    ExifInterface.ORIENTATION_TRANSVERSE -> TransformacionExif(270, true)
    else -> TransformacionExif(0, false)
}

/**
 * Prueba las [calidades] en orden y devuelve la primera salida que pesa [objetivo] o menos.
 * Si ninguna llega, devuelve la más chica. [codificar] es la compresión real (JPEG).
 */
internal fun comprimirHastaObjetivo(
    calidades: List<Int>,
    objetivo: Int,
    codificar: (Int) -> ByteArray
): ByteArray {
    var mejor: ByteArray? = null
    for (calidad in calidades) {
        val salida = codificar(calidad)
        if (mejor == null || salida.size < mejor.size) mejor = salida
        if (salida.size <= objetivo) return salida
    }
    return requireNotNull(mejor) { "Sin calidades para probar" }
}

/**
 * Decide qué hacer con la foto ya medida: si no hace falta tocarla se sube igual, y si hay que
 * achicarla, [achicar] devuelve los bytes nuevos (o null si no se pudo decodificar).
 */
internal fun prepararFoto(
    original: ByteArray,
    ancho: Int,
    alto: Int,
    transformacion: TransformacionExif,
    achicar: () -> ByteArray?
): FotoPreparada {
    val yaAlcanza = ancho > 0 && alto > 0 &&
        maxOf(ancho, alto) <= LADO_MAXIMO_PX &&
        original.size <= PESO_OBJETIVO_BYTES &&
        transformacion.esIdentidad
    if (yaAlcanza) return FotoPreparada.Lista(original)
    val achicada = achicar()
    return when {
        achicada != null && achicada.size <= PESO_MAXIMO_BYTES -> FotoPreparada.Lista(achicada)
        // No es una imagen que se pueda leer: se sube tal cual solo si no es enorme.
        achicada == null && original.size <= PESO_MAXIMO_BYTES -> FotoPreparada.Lista(original)
        else -> FotoPreparada.DemasiadoGrande
    }
}

/** Achica, corrige la orientación EXIF y recomprime a JPEG. Pesado: llamar fuera del hilo principal. */
internal fun prepararFoto(original: ByteArray): FotoPreparada {
    val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(original, 0, original.size, limites)
    val transformacion = leerOrientacion(original)
    return prepararFoto(original, limites.outWidth, limites.outHeight, transformacion) {
        achicar(original, limites.outWidth, limites.outHeight, transformacion)
    }
}

private fun leerOrientacion(bytes: ByteArray): TransformacionExif = try {
    val orientacion = ExifInterface(ByteArrayInputStream(bytes))
        .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    transformacionExif(orientacion)
} catch (e: Exception) {
    Log.w(TAG, "No se pudo leer la orientación EXIF", e)
    TransformacionExif(0, false)
}

private fun achicar(original: ByteArray, ancho: Int, alto: Int, transformacion: TransformacionExif): ByteArray? {
    if (ancho <= 0 || alto <= 0) return null
    val opciones = BitmapFactory.Options().apply { inSampleSize = calcularInSampleSize(ancho, alto, LADO_MAXIMO_PX) }
    val decodificada = BitmapFactory.decodeByteArray(original, 0, original.size, opciones) ?: return null
    var actual: Bitmap = decodificada
    try {
        val (anchoFinal, altoFinal) = dimensionesDestino(actual.width, actual.height, LADO_MAXIMO_PX)
        val matriz = Matrix().apply {
            if (anchoFinal != actual.width) postScale(anchoFinal.toFloat() / actual.width, altoFinal.toFloat() / actual.height)
            if (transformacion.espejar) postScale(-1f, 1f)
            if (transformacion.grados != 0) postRotate(transformacion.grados.toFloat())
        }
        val transformada = Bitmap.createBitmap(actual, 0, 0, actual.width, actual.height, matriz, true)
        if (transformada !== actual) {
            actual.recycle()
            actual = transformada
        }
        return comprimirHastaObjetivo(CALIDADES_JPEG, PESO_OBJETIVO_BYTES) { calidad ->
            ByteArrayOutputStream().also { actual.compress(Bitmap.CompressFormat.JPEG, calidad, it) }.toByteArray()
        }
    } finally {
        actual.recycle()
    }
}
