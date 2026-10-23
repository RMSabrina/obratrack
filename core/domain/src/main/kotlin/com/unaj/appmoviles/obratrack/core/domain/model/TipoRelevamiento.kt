package com.unaj.appmoviles.obratrack.core.domain.model

object TipoRelevamiento {
    const val AVANCE = "Avance de Obra"
    const val SEGURIDAD = "Seguridad e Higiene"
    const val CALIDAD = "Control de Calidad"
    const val MATERIALES = "Recepción de Materiales"
    const val INCIDENCIA = "Incidencia Técnica"

    val TODOS = listOf(
        AVANCE,
        SEGURIDAD,
        CALIDAD,
        MATERIALES,
        INCIDENCIA
    )

    val CHECKLIST_DEFAULT = listOf(
        "Protocolos de seguridad y EPP verificados",
        "Personal y dotación presentes en regla",
        "Limpieza, orden y acopio de materiales",
        "Trabajos conformes a planos / especificaciones"
    )
}
