package com.unaj.appmoviles.features.home.agenda

import com.unaj.appmoviles.obratrack.core.domain.error.AppResult
import com.unaj.appmoviles.obratrack.core.domain.error.ObraError
import org.junit.Assert.assertEquals
import org.junit.Test

class FeedbackAgendaTest {

    @Test
    fun exitoConFecha_avisaVisitaProgramada() {
        val feedback = feedbackDeReprogramacion("Casa Pérez", 1_700_000_000_000L, AppResult.Success(Unit))
        assertEquals(FeedbackAgenda.VisitaProgramada("Casa Pérez"), feedback)
    }

    @Test
    fun exitoSinFecha_avisaVisitaCancelada() {
        val feedback = feedbackDeReprogramacion("Casa Pérez", null, AppResult.Success(Unit))
        assertEquals(FeedbackAgenda.VisitaCancelada("Casa Pérez"), feedback)
    }

    @Test
    fun error_conservaElErrorDelDominio() {
        val feedback = feedbackDeReprogramacion("Casa Pérez", 1_700_000_000_000L, AppResult.Error(ObraError.NoAutorizada))
        assertEquals(FeedbackAgenda.ErrorAlProgramar(ObraError.NoAutorizada), feedback)
    }
}
