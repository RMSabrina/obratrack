package com.unaj.appmoviles.features.obras

import android.content.Context
import android.net.Uri
import android.util.Log
import com.unaj.appmoviles.features.obras.relevamiento.RelevamientoFormViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream

private const val TAG = "FotoUi"

/**
 * Lee la foto elegida (ruta de archivo o Uri de contenido) en IO. Nunca devuelve más de
 * `MAX_FOTO_ORIGINAL_BYTES + 1` bytes: una foto más grande llega truncada pero con un tamaño que
 * el ViewModel rechaza, y no se carga entera en memoria. null si no se pudo leer.
 */
suspend fun leerFoto(context: Context, rutaOUri: String?): ByteArray? {
    if (rutaOUri.isNullOrBlank()) return null
    return withContext(Dispatchers.IO) {
        try {
            val limite = RelevamientoFormViewModel.MAX_FOTO_ORIGINAL_BYTES
            val archivo = if (rutaOUri.startsWith("content://")) null else File(rutaOUri.removePrefix("file://"))
            if (archivo != null && archivo.exists() && archivo.length() > 0) {
                archivo.inputStream().use { it.leerConLimite(limite, archivo.length()) }
            } else {
                val uri = Uri.parse(rutaOUri)
                val tamano = context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L
                context.contentResolver.openInputStream(uri)?.use { it.leerConLimite(limite, tamano) }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "No se pudo leer la foto elegida", e)
            null
        }
    }
}

/**
 * Lee como mucho `limite + 1` bytes. Con el [tamanoConocido] reserva justo lo necesario (sin copias
 * intermedias); sin él, va acumulando.
 */
internal fun InputStream.leerConLimite(limite: Int, tamanoConocido: Long = -1L): ByteArray {
    val tope = limite + 1
    if (tamanoConocido in 1..tope.toLong()) {
        val exacto = ByteArray(tamanoConocido.toInt())
        var leidos = 0
        while (leidos < exacto.size) {
            val n = read(exacto, leidos, exacto.size - leidos)
            if (n < 0) break
            leidos += n
        }
        return if (leidos == exacto.size) exacto else exacto.copyOf(leidos)
    }
    val salida = ByteArrayOutputStream()
    val buffer = ByteArray(16 * 1024)
    var total = 0
    while (total < tope) {
        val n = read(buffer, 0, minOf(buffer.size, tope - total))
        if (n < 0) break
        salida.write(buffer, 0, n)
        total += n
    }
    return salida.toByteArray()
}
