package com.unaj.appmoviles.features.obras.export

import android.util.Log
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

private const val TAG = "CacheTemporal"

private val PREFIJOS_PDF = listOf("acta_visita_", "informe_obra_")
private const val PREFIJO_FOTO = "relevamiento-"

/** Un PDF ya compartido o una foto ya subida no hacen falta; un día alcanza para que la otra app lo lea. */
internal val EDAD_MAXIMA_TEMPORAL_MS = TimeUnit.DAYS.toMillis(1)

/**
 * Rutas de las fotos que un formulario abierto todavía usa. La limpieza no las borra aunque sean
 * viejas (por ejemplo, un formulario que quedó abierto de un día para el otro).
 */
object FotosEnUso {
    private val rutas: MutableSet<String> = ConcurrentHashMap.newKeySet()

    private fun normalizar(ruta: String) = ruta.removePrefix("file://")

    fun registrar(ruta: String) {
        rutas.add(normalizar(ruta))
    }

    fun liberar(ruta: String) {
        rutas.remove(normalizar(ruta))
    }

    fun rutas(): Set<String> = rutas.toSet()
}

private fun vencidos(
    carpeta: File,
    ahoraMs: Long,
    edadMaximaMs: Long,
    enUso: Set<String>,
    esPropio: (String) -> Boolean
): List<File> =
    carpeta.listFiles().orEmpty().filter { archivo ->
        archivo.isFile &&
            esPropio(archivo.name) &&
            archivo.absolutePath !in enUso &&
            ahoraMs - archivo.lastModified() > edadMaximaMs
    }

/** PDF generados (acta e informe) con más de [edadMaximaMs] en [carpeta]. */
internal fun pdfsTemporalesVencidos(
    carpeta: File,
    ahoraMs: Long,
    edadMaximaMs: Long = EDAD_MAXIMA_TEMPORAL_MS
): List<File> =
    vencidos(carpeta, ahoraMs, edadMaximaMs, emptySet()) { nombre ->
        PREFIJOS_PDF.any { nombre.startsWith(it) } && nombre.endsWith(".pdf")
    }

/** Fotos de la cámara con más de [edadMaximaMs] que ningún formulario ([enUso]) referencia. */
internal fun fotosTemporalesVencidas(
    carpeta: File,
    ahoraMs: Long,
    enUso: Set<String>,
    edadMaximaMs: Long = EDAD_MAXIMA_TEMPORAL_MS
): List<File> =
    vencidos(carpeta, ahoraMs, edadMaximaMs, enUso) { nombre ->
        nombre.startsWith(PREFIJO_FOTO) && nombre.endsWith(".jpg")
    }

private fun borrar(archivos: List<File>) {
    for (archivo in archivos) {
        if (!archivo.delete()) Log.w(TAG, "No se pudo borrar ${archivo.name}")
    }
}

/** Al iniciar la app: ningún formulario usa un PDF. Llamar fuera del hilo principal. */
fun limpiarPdfsTemporales(carpeta: File, ahoraMs: Long = System.currentTimeMillis()) {
    borrar(pdfsTemporalesVencidos(carpeta, ahoraMs))
}

/** Al terminar de guardar un relevamiento. Llamar fuera del hilo principal. */
fun limpiarFotosTemporales(
    carpeta: File,
    enUso: Set<String> = FotosEnUso.rutas(),
    ahoraMs: Long = System.currentTimeMillis()
) {
    borrar(fotosTemporalesVencidas(carpeta, ahoraMs, enUso))
}
