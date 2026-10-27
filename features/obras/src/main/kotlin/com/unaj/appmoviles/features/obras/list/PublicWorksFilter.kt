package com.unaj.appmoviles.features.obras.list

import com.unaj.appmoviles.obratrack.core.domain.model.EstadoObra
import com.unaj.appmoviles.obratrack.core.domain.model.Obra
import java.text.Normalizer
import java.util.Locale

internal fun filtrarObrasPublicas(
    obras: List<Obra>,
    query: String,
    estado: EstadoObra?,
    suscripciones: Set<String> = emptySet(),
    soloSuscripciones: Boolean = false
): List<Obra> {
    val consulta = normalizar(query.trim())
    return obras.filter { obra ->
        val coincideTexto = consulta.isEmpty() || listOf(obra.nombre, obra.cliente, obra.direccion)
            .any { normalizar(it).contains(consulta) }
        coincideTexto &&
            (estado == null || obra.estado == estado) &&
            (!soloSuscripciones || obra.id in suscripciones)
    }
}

private fun normalizar(value: String): String =
    Normalizer.normalize(value.lowercase(Locale.ROOT), Normalizer.Form.NFD)
        .replace("\\p{Mn}+".toRegex(), "")
