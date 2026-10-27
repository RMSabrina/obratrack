package com.unaj.appmoviles.obratrack.core.data.fotos

import java.io.File

/**
 * Fotos en espera de subirse, una carpeta por obra: <raiz>/<obraId>/<relevamientoId>.jpg.
 * Vive en filesDir (no en la caché), así que el sistema y la limpieza de temporales no la tocan.
 */
class FotosPendientesStore(private val raiz: File) {

    /** Guarda (o reemplaza) la foto. Escribe en un temporal y renombra, para no dejar un archivo a medias. */
    fun guardar(obraId: String, relevamientoId: String, bytes: ByteArray): File {
        val carpeta = File(raiz, obraId).also { it.mkdirs() }
        val destino = File(carpeta, "$relevamientoId.jpg")
        val temporal = File(carpeta, "$relevamientoId.jpg.tmp")
        temporal.writeBytes(bytes)
        if (!temporal.renameTo(destino)) {
            destino.delete()
            check(temporal.renameTo(destino)) { "No se pudo guardar la foto pendiente" }
        }
        return destino
    }

    fun archivo(obraId: String, relevamientoId: String): File? =
        File(File(raiz, obraId), "$relevamientoId.jpg").takeIf { it.isFile }

    fun borrar(obraId: String, relevamientoId: String) {
        File(File(raiz, obraId), "$relevamientoId.jpg").delete()
        File(File(raiz, obraId), "$relevamientoId.jpg.tmp").delete()
    }

    fun borrarObra(obraId: String) {
        File(raiz, obraId).deleteRecursively()
    }
}
