package com.unaj.appmoviles.features.obras

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.util.Locale
import java.util.TimeZone

class FechaUiTest {

    private val zonaOriginal: TimeZone = TimeZone.getDefault()

    @After
    fun tearDown() {
        TimeZone.setDefault(zonaOriginal)
    }

    @Test
    fun millisUtcToLocalDate_medianocheUtc_devuelveEseDia() {
        val millis = Instant.parse("2026-10-21T00:00:00Z").toEpochMilli()

        assertEquals(LocalDate.of(2026, 10, 21), millisUtcToLocalDate(millis))
    }

    @Test
    fun millisUtcToLocalDate_enArgentina_noRestaUnDia() {
        // En UTC-3, la medianoche UTC del 21 son las 21 h del 20: convertir con la zona
        // del dispositivo daría el día anterior.
        TimeZone.setDefault(TimeZone.getTimeZone("America/Argentina/Buenos_Aires"))
        val millis = Instant.parse("2026-10-21T00:00:00Z").toEpochMilli()

        assertEquals(LocalDate.of(2026, 10, 21), millisUtcToLocalDate(millis))
    }

    @Test
    fun toMillisUtc_devuelveMedianocheUtc() {
        TimeZone.setDefault(TimeZone.getTimeZone("America/Argentina/Buenos_Aires"))

        assertEquals(
            Instant.parse("2026-10-21T00:00:00Z").toEpochMilli(),
            LocalDate.of(2026, 10, 21).toMillisUtc()
        )
    }

    @Test
    fun conversion_idaYVuelta_conservaLaFecha() {
        TimeZone.setDefault(TimeZone.getTimeZone("America/Argentina/Buenos_Aires"))
        val fechas = listOf(
            LocalDate.of(1970, 1, 1),
            LocalDate.of(2024, 2, 29),
            LocalDate.of(2026, 10, 21),
            LocalDate.of(2026, 12, 31)
        )

        fechas.forEach { fecha ->
            assertEquals(fecha, millisUtcToLocalDate(fecha.toMillisUtc()))
        }
    }

    private val es = Locale.forLanguageTag("es")
    private val en = Locale.ENGLISH
    private val it = Locale.ITALIAN

    @Test
    fun formatFecha_localDate_usaMesesDelIdioma() {
        val fecha = LocalDate.of(2026, 1, 5)

        assertEquals("5 ene 2026", formatFecha(fecha, es))
        assertEquals("Jan 5, 2026", formatFecha(fecha, en))
        assertEquals("5 gen 2026", formatFecha(fecha, it))
    }

    @Test
    fun formatFecha_millisUtc_leeElDiaEnUtcYUsaElIdioma() {
        TimeZone.setDefault(TimeZone.getTimeZone("America/Argentina/Buenos_Aires"))
        val millis = Instant.parse("2026-10-26T00:00:00Z").toEpochMilli()

        assertEquals("26 oct 2026", formatFecha(millis, es))
        assertEquals("Oct 26, 2026", formatFecha(millis, en))
        assertEquals("26 ott 2026", formatFecha(millis, it))
    }

    @Test
    fun formatDiaMes_respetaIdiomaYOrden() {
        val fecha = LocalDate.of(2026, 10, 26)

        assertEquals("26 oct", formatDiaMes(fecha, es))
        assertEquals("Oct 26", formatDiaMes(fecha, en))
        assertEquals("26 ott", formatDiaMes(fecha, it))
    }
}
