package com.unaj.appmoviles.obratrack.core.firebase

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

/** La próxima visita vive en la obra: un relevamiento solo la propone al crearse. */
class CamposVisitaObraTest {

    private val visitaMillis = 1_793_059_200_000L // 2026-10-27, medianoche UTC
    private val fechaPasadaMillis = 1_700_000_000_000L // 2023-11-14

    @Test
    fun editarRelevamientoViejoConVisita_noCambiaLaObra() {
        assertTrue(camposVisitaObra(alCrear = false, visitaMillis, 600).isEmpty())
    }

    @Test
    fun editarRelevamientoConFechaPasada_noPisaLaVisitaVigente() {
        assertTrue(camposVisitaObra(alCrear = false, fechaPasadaMillis, null).isEmpty())
    }

    @Test
    fun crearRelevamientoConVisita_laEscribeEnLaObra() {
        val campos = camposVisitaObra(alCrear = true, visitaMillis, 600)

        assertEquals(Timestamp(Date(visitaMillis)), campos["proximaVisita"])
        assertEquals(600, campos["horaVisita"])
    }

    @Test
    fun crearRelevamientoConVisitaSinHora_borraLaHoraVieja() {
        val campos = camposVisitaObra(alCrear = true, visitaMillis, null)

        assertEquals(FieldValue.delete(), campos["horaVisita"])
    }

    @Test
    fun crearRelevamientoSinVisita_noLaBorra() {
        assertTrue(camposVisitaObra(alCrear = true, null, null).isEmpty())
    }
}
