package com.unaj.appmoviles.obratrack.core.firebase

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.toFechaCalendario
import com.unaj.appmoviles.obratrack.core.domain.usecase.obras.toMillisCalendario
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.util.Date

/** La última visita de la obra es la fecha del relevamiento más reciente que queda. */
class UltimaVisitaObraTest {

    private fun dia(d: Int) = LocalDate.of(2026, 10, d).toMillisCalendario()

    private val fechas = mapOf("a" to dia(5), "b" to dia(12), "c" to dia(20))

    @Test
    fun sinCambios_esLaMasReciente() {
        assertEquals(dia(20), ultimaVisitaTras(fechas))
    }

    @Test
    fun borrarElUnicoRelevamiento_noDejaUltimaVisita() {
        assertNull(ultimaVisitaTras(mapOf("a" to dia(5)), quitar = setOf("a")))
    }

    @Test
    fun borrarElMasReciente_quedaElAnterior() {
        assertEquals(dia(12), ultimaVisitaTras(fechas, quitar = setOf("c")))
    }

    @Test
    fun borrarUnoQueNoEsElUltimo_noCambiaLaUltimaVisita() {
        assertEquals(dia(20), ultimaVisitaTras(fechas, quitar = setOf("a")))
        assertEquals(dia(20), ultimaVisitaTras(fechas, quitar = setOf("b")))
    }

    @Test
    fun borrarTodos_noDejaUltimaVisita() {
        assertNull(ultimaVisitaTras(fechas, quitar = fechas.keys))
    }

    @Test
    fun borrarVariosVinculadosDeUnaTarea_quedaElMasRecienteQueNoSeBorra() {
        // Tarea con "b" y "c" vinculados: se borran los dos.
        assertEquals(dia(5), ultimaVisitaTras(fechas, quitar = setOf("b", "c")))
    }

    @Test
    fun obraSinRelevamientos_noTieneUltimaVisita() {
        assertNull(ultimaVisitaTras(emptyMap()))
    }

    @Test
    fun editarElMasRecienteHaciaAtras_pasaAserElSiguiente() {
        assertEquals(dia(12), ultimaVisitaTras(fechas, cambiar = "c" to dia(8)))
    }

    @Test
    fun editarElMasRecienteHaciaAdelante_subeLaUltimaVisita() {
        assertEquals(dia(25), ultimaVisitaTras(fechas, cambiar = "c" to dia(25)))
    }

    @Test
    fun editarUnoViejoAUnaFechaPosterior_pasaAserLaUltima() {
        assertEquals(dia(28), ultimaVisitaTras(fechas, cambiar = "a" to dia(28)))
    }

    @Test
    fun editarUnoViejoSinSuperarElMaximo_noCambiaLaUltimaVisita() {
        assertEquals(dia(20), ultimaVisitaTras(fechas, cambiar = "a" to dia(7)))
    }

    @Test
    fun medianocheUtc_elDiaDelResultadoCoincideConToFechaCalendario() {
        val dias = listOf(LocalDate.of(2026, 1, 31), LocalDate.of(2026, 3, 1), LocalDate.of(2025, 12, 31))
        val porId = dias.mapIndexed { i, d -> "r$i" to d.toMillisCalendario() }.toMap()

        val ultima = ultimaVisitaTras(porId)!!

        assertEquals(dias.max(), ultima.toFechaCalendario())
        assertEquals(dias.max().toMillisCalendario(), ultima)
    }

    @Test
    fun datosConHora_elMaximoEnMillisEsElMaximoPorDia() {
        // Un dato viejo podría traer hora: el orden por millis nunca contradice el orden por día.
        val finDelDia20 = dia(20) + 24L * 3600 * 1000 - 1
        val inicioDelDia21 = dia(21)
        val mediaTarde12 = dia(12) + 15L * 3600 * 1000
        val porId = mapOf("x" to finDelDia20, "y" to inicioDelDia21, "z" to mediaTarde12)

        val ultima = ultimaVisitaTras(porId)!!
        val diaMaximo = porId.values.map { it.toFechaCalendario() }.max()

        assertEquals(diaMaximo, ultima.toFechaCalendario())
        assertEquals(LocalDate.of(2026, 10, 21), ultima.toFechaCalendario())
    }

    @Test
    fun valor_conFecha_esUnTimestamp() {
        assertEquals(Timestamp(Date(dia(20))), valorUltimaVisita(dia(20)))
    }

    @Test
    fun valor_sinFecha_borraElCampo() {
        assertEquals(FieldValue.delete(), valorUltimaVisita(null))
    }
}
