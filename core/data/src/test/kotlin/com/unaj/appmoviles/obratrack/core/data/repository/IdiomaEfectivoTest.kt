package com.unaj.appmoviles.obratrack.core.data.repository

import com.unaj.appmoviles.obratrack.core.domain.model.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IdiomaEfectivoTest {

    @Test
    fun sinEleccionYSistemaEnIngles_esIngles() {
        // El caso del bug: la app se ve en inglés y el selector marcaba español.
        assertEquals(
            AppLanguage.ENGLISH,
            idiomaEfectivo(emptyList(), espejo = null, idiomasSistema = listOf("en"), usarEspejo = true)
        )
    }

    @Test
    fun sinEleccionYSistemaEnItaliano_esItaliano() {
        assertEquals(
            AppLanguage.ITALIAN,
            idiomaEfectivo(emptyList(), null, listOf("it"), usarEspejo = false)
        )
    }

    @Test
    fun sistemaConIdiomaNoSoportado_caeEnElPorDefecto() {
        assertEquals(
            AppLanguage.SPANISH,
            idiomaEfectivo(emptyList(), null, listOf("fr"), usarEspejo = true)
        )
    }

    @Test
    fun sistemaConVariosIdiomas_tomaElPrimeroSoportado() {
        assertEquals(
            AppLanguage.ENGLISH,
            idiomaEfectivo(emptyList(), null, listOf("fr", "en", "it"), usarEspejo = false)
        )
    }

    @Test
    fun sinNadaDeNada_esEspanol() {
        assertEquals(AppLanguage.SPANISH, idiomaEfectivo(emptyList(), null, emptyList(), usarEspejo = true))
    }

    @Test
    fun elIdiomaDeLaAppGanaSobreElEspejoYElSistema() {
        assertEquals(
            AppLanguage.ITALIAN,
            idiomaEfectivo(listOf("it"), espejo = "en", idiomasSistema = listOf("en"), usarEspejo = true)
        )
    }

    @Test
    fun idiomaDeLaAppNoSoportado_caeEnEspanolComoLosRecursos() {
        assertEquals(
            AppLanguage.SPANISH,
            idiomaEfectivo(listOf("fr"), espejo = null, idiomasSistema = listOf("en"), usarEspejo = true)
        )
    }

    @Test
    fun conEspejo_vaAntesQueElSistema() {
        assertEquals(
            AppLanguage.ITALIAN,
            idiomaEfectivo(emptyList(), espejo = "it", idiomasSistema = listOf("en"), usarEspejo = true)
        )
    }

    @Test
    fun sinUsarElEspejo_unEspejoViejoNoPisaAlSistema() {
        // Android 13+: si se vuelve a "idioma del sistema", un espejo viejo no debe reaparecer.
        assertEquals(
            AppLanguage.ENGLISH,
            idiomaEfectivo(emptyList(), espejo = "es", idiomasSistema = listOf("en"), usarEspejo = false)
        )
    }

    // --- espejoTrasSincronizar ---

    @Test
    fun espejo_conIdiomaElegido_seGuarda() {
        assertEquals("en", espejoTrasSincronizar(listOf("en-US"), espejoActual = "es", appCompatEsLaFuente = true))
        assertEquals("it", espejoTrasSincronizar(listOf("it"), espejoActual = null, appCompatEsLaFuente = false))
    }

    @Test
    fun espejo_sinEleccionEnAndroid13oMas_seBorra() {
        assertNull(espejoTrasSincronizar(emptyList(), espejoActual = "es", appCompatEsLaFuente = true))
    }

    @Test
    fun espejo_sinEleccionEnAndroid12oMenos_seConserva() {
        assertEquals("it", espejoTrasSincronizar(emptyList(), espejoActual = "it", appCompatEsLaFuente = false))
        assertNull(espejoTrasSincronizar(emptyList(), espejoActual = null, appCompatEsLaFuente = false))
    }

    @Test
    fun espejo_conIdiomaNoSoportado_seBorra() {
        assertNull(espejoTrasSincronizar(listOf("fr"), espejoActual = "es", appCompatEsLaFuente = false))
    }
}
