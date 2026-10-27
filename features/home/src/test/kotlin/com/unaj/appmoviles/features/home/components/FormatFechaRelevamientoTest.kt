package com.unaj.appmoviles.features.home.components

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import java.util.TimeZone

/** La fecha de calendario se lee en UTC: en Argentina (UTC-3) no debe correrse al día anterior. */
class FormatFechaRelevamientoTest {

    private val zonaOriginal = TimeZone.getDefault()
    private val fecha26Oct = 1_792_972_800_000L // 2026-10-26, medianoche UTC

    @Before
    fun setUp() {
        TimeZone.setDefault(TimeZone.getTimeZone("America/Argentina/Buenos_Aires"))
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(zonaOriginal)
    }

    private fun esperado(locale: Locale) = LocalDate.of(2026, 10, 26)
        .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))

    @Test
    fun enArgentina_muestraElDiaElegido_sinHora() {
        val locale = Locale.forLanguageTag("es-AR")
        val texto = formatFechaRelevamiento(fecha26Oct, locale)

        assertEquals(esperado(locale), texto)
        assertFalse(texto.contains("25"))
        assertFalse(texto.contains("21:00"))
    }

    @Test
    fun usaElFormatoDelIdiomaElegido() {
        assertEquals(esperado(Locale.ENGLISH), formatFechaRelevamiento(fecha26Oct, Locale.ENGLISH))
        assertEquals(esperado(Locale.ITALIAN), formatFechaRelevamiento(fecha26Oct, Locale.ITALIAN))
        // Antes el formato era es-AR fijo para todos los idiomas.
        assertNotEquals(
            formatFechaRelevamiento(fecha26Oct, Locale.forLanguageTag("es-AR")),
            formatFechaRelevamiento(fecha26Oct, Locale.ENGLISH)
        )
    }
}
