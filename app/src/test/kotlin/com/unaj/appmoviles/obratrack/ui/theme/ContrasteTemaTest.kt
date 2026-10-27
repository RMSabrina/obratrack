package com.unaj.appmoviles.obratrack.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

/**
 * Contraste WCAG de los pares de colores que la app usa de verdad, en el tema claro y en el oscuro.
 * Texto: 4,5:1. Bordes, íconos y relleno que informa algo: 3:1.
 */
class ContrasteTemaTest {

    private val esquemas = listOf("claro" to LightColorScheme, "oscuro" to DarkColorScheme)

    private fun luminancia(c: Color): Double {
        fun lineal(v: Float): Double {
            val x = v.toDouble()
            return if (x <= 0.04045) x / 12.92 else ((x + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * lineal(c.red) + 0.7152 * lineal(c.green) + 0.0722 * lineal(c.blue)
    }

    private fun contraste(a: Color, b: Color): Double {
        val la = luminancia(a)
        val lb = luminancia(b)
        return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
    }

    private class Par(val nombre: String, val primero: ColorScheme.() -> Color, val segundo: ColorScheme.() -> Color)

    private fun par(nombre: String, a: ColorScheme.() -> Color, b: ColorScheme.() -> Color) = Par(nombre, a, b)

    private fun verificar(minimo: Double, pares: List<Par>) {
        val fallas = buildList {
            for ((tema, esquema) in esquemas) {
                for (p in pares) {
                    val valor = contraste(esquema.(p.primero)(), esquema.(p.segundo)())
                    if (valor < minimo) add("%s: %s = %.2f (mínimo %.1f)".format(tema, p.nombre, valor, minimo))
                }
            }
        }
        assertTrue("Contraste insuficiente:\n" + fallas.joinToString("\n"), fallas.isEmpty())
    }

    @Test
    fun textoSobreLasSuperficies() = verificar(
        4.5,
        listOf(
            par("onSurface/surface", { onSurface }, { surface }),
            par("onSurface/surfaceContainerLowest", { onSurface }, { surfaceContainerLowest }),
            par("onSurface/surfaceContainerLow", { onSurface }, { surfaceContainerLow }),
            par("onSurface/surfaceContainer", { onSurface }, { surfaceContainer }),
            par("onSurface/surfaceContainerHigh", { onSurface }, { surfaceContainerHigh }),
            par("onSurface/surfaceContainerHighest", { onSurface }, { surfaceContainerHighest }),
            par("onSurfaceVariant/surface", { onSurfaceVariant }, { surface }),
            par("onSurfaceVariant/surfaceContainerLowest", { onSurfaceVariant }, { surfaceContainerLowest }),
            par("onSurfaceVariant/surfaceContainerLow", { onSurfaceVariant }, { surfaceContainerLow }),
            par("onSurfaceVariant/surfaceContainer", { onSurfaceVariant }, { surfaceContainer }),
            par("onSurfaceVariant/surfaceContainerHigh", { onSurfaceVariant }, { surfaceContainerHigh }),
            par("onSurfaceVariant/surfaceContainerHighest", { onSurfaceVariant }, { surfaceContainerHighest }),
            par("onSurfaceVariant/surfaceVariant", { onSurfaceVariant }, { surfaceVariant }),
            par("onBackground/background", { onBackground }, { background }),
            par("inverseOnSurface/inverseSurface", { inverseOnSurface }, { inverseSurface })
        )
    )

    @Test
    fun textoSobreLosColoresDeMarcaYDeError() = verificar(
        4.5,
        listOf(
            par("onPrimary/primary", { onPrimary }, { primary }),
            par("onPrimaryContainer/primaryContainer", { onPrimaryContainer }, { primaryContainer }),
            par("onSecondary/secondary", { onSecondary }, { secondary }),
            par("onSecondaryContainer/secondaryContainer", { onSecondaryContainer }, { secondaryContainer }),
            par("onTertiary/tertiary", { onTertiary }, { tertiary }),
            par("onTertiaryContainer/tertiaryContainer", { onTertiaryContainer }, { tertiaryContainer }),
            par("onError/error", { onError }, { error }),
            par("onErrorContainer/errorContainer", { onErrorContainer }, { errorContainer })
        )
    )

    @Test
    fun textoDeColorSobreLaSuperficie() = verificar(
        4.5,
        listOf(
            par("primary/surface", { primary }, { surface }),
            par("primary/surfaceContainerLow", { primary }, { surfaceContainerLow }),
            par("error/surface", { error }, { surface }),
            par("error/surfaceContainerLow", { error }, { surfaceContainerLow })
        )
    )

    @Test
    fun bordesIconosYAcentosSobreLaSuperficie() = verificar(
        3.0,
        listOf(
            par("outline/surface", { outline }, { surface }),
            par("outline/surfaceContainerLow", { outline }, { surfaceContainerLow }),
            par("outline/surfaceContainerHighest", { outline }, { surfaceContainerHighest }),
            par("primary/surface", { primary }, { surface }),
            par("secondary/surface", { secondary }, { surface }),
            par("tertiary/surface", { tertiary }, { surface }),
            par("error/surface", { error }, { surface })
        )
    )

    @Test
    fun ganttEnCurso_elBordeSeLeeSobreSuRelleno() = verificar(
        3.0,
        listOf(par("primary/secondaryContainer", { primary }, { secondaryContainer }))
    )

    @Test
    fun lasTarjetasSeSeparanDelFondoPorTono_noPorContraste() {
        // surfaceContainerLow es una tarjeta sobre surface: el contraste es bajo a propósito.
        for ((tema, esquema) in esquemas) {
            val valor = contraste(esquema.surfaceContainerLow, esquema.surface)
            assertTrue("$tema: la tarjeta no debería ser idéntica al fondo", esquema.surfaceContainerLow != esquema.surface)
            assertTrue("$tema: la tarjeta se separa demasiado del fondo (%.2f)".format(valor), valor < 1.3)
        }
    }
}
