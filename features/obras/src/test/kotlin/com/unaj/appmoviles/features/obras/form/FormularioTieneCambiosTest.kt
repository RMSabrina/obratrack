package com.unaj.appmoviles.features.obras.form

import com.unaj.appmoviles.features.obras.relevamiento.RelevamientoFormInitial
import com.unaj.appmoviles.features.obras.relevamiento.relevamientoTieneCambios
import com.unaj.appmoviles.features.obras.tareas.form.tareaTieneCambios
import com.unaj.appmoviles.obratrack.core.domain.model.CategoriaTarea
import com.unaj.appmoviles.obratrack.core.domain.model.EstadoTarea
import com.unaj.appmoviles.obratrack.core.domain.model.TareaDraft
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class FormularioTieneCambiosTest {

    private val obra = ObraFormInitial(
        obraId = "o1", nombre = "Casa", cliente = "Ana", direccion = "Calle 1",
        estado = "ACTIVA", publica = false, latitud = 1.0, longitud = 2.0, proximaVisita = 10L, horaVisita = 600
    )

    private fun obraCon(nombre: String = obra.nombre, publica: Boolean = obra.publica, hora: Int? = obra.horaVisita) =
        obraTieneCambios(obra, nombre, obra.cliente, obra.direccion, obra.estado, publica, obra.latitud, obra.longitud, obra.proximaVisita, hora)

    @Test
    fun obra_sinTocarNada_noHayCambios() = assertFalse(obraCon())

    @Test
    fun obra_cualquierCampoDistinto_hayCambios() {
        assertTrue(obraCon(nombre = "Casa 2"))
        assertTrue(obraCon(publica = true))
        assertTrue(obraCon(hora = null))
    }

    private val rel = RelevamientoFormInitial(
        obraId = "o1", relevamientoId = null, nota = "", fechaMillis = 0L, proximaVisita = null
    )

    private fun relCon(nota: String = "", fecha: Long = HOY, foto: String? = null) =
        relevamientoTieneCambios(rel, HOY, nota, fecha, null, null, foto, null, null, null)

    @Test
    fun relevamiento_nuevoSinTocar_usaHoyComoFechaInicial() = assertFalse(relCon())

    @Test
    fun relevamiento_notaFechaOFotoNueva_hayCambios() {
        assertTrue(relCon(nota = "Avance"))
        assertTrue(relCon(fecha = HOY + 1))
        assertTrue(relCon(foto = "content://foto"))
    }

    private val tarea = TareaDraft(titulo = "Colar", fechaInicio = LocalDate.of(2026, 1, 1), fechaFin = LocalDate.of(2026, 1, 5))

    private fun tareaCon(titulo: String = tarea.titulo, estado: EstadoTarea = tarea.estado, fin: LocalDate? = tarea.fechaFin) =
        tareaTieneCambios(tarea, titulo, tarea.descripcion, CategoriaTarea.OTROS, estado, tarea.fechaInicio, fin)

    @Test
    fun tarea_sinTocarNada_noHayCambios() = assertFalse(tareaCon())

    @Test
    fun tarea_tituloEstadoOFechaDistinta_hayCambios() {
        assertTrue(tareaCon(titulo = "Otra"))
        assertTrue(tareaCon(estado = EstadoTarea.COMPLETADA))
        assertTrue(tareaCon(fin = null))
    }

    private companion object {
        const val HOY = 1_700_000_000_000L
    }
}
