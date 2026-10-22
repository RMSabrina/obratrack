package com.unaj.appmoviles.obratrack.core.data.repository

import com.unaj.appmoviles.obratrack.core.domain.error.TareaError
import com.unaj.appmoviles.obratrack.core.domain.model.CategoriaTarea
import com.unaj.appmoviles.obratrack.core.domain.model.EstadoTarea
import com.unaj.appmoviles.obratrack.core.domain.model.TareaDraft
import com.unaj.appmoviles.obratrack.core.firebase.ObraForbiddenException
import com.unaj.appmoviles.obratrack.core.firebase.ObraNotFoundException
import com.unaj.appmoviles.obratrack.core.firebase.TareaDto
import com.unaj.appmoviles.obratrack.core.firebase.TareaForbiddenException
import com.unaj.appmoviles.obratrack.core.firebase.TareaNotFoundException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class TareaMappersTest {

    private val inicio = LocalDate.of(2026, 10, 21)
    private val fin = LocalDate.of(2026, 10, 30)

    @Test
    fun toDto_escribeFechasIsoYNombresDeEnums() {
        val dto = draft(fechaInicio = inicio, fechaFin = fin).toDto("uid", "obra-1")

        assertEquals("2026-10-21", dto.fechaInicio)
        assertEquals("2026-10-30", dto.fechaFin)
        assertEquals("ELECTRICIDAD", dto.categoria)
        assertEquals("EN_CURSO", dto.estado)
        assertEquals("uid", dto.userId)
        assertEquals("obra-1", dto.obraId)
    }

    @Test
    fun idaYVuelta_conFechas_conservaLosDatos() {
        val original = draft(fechaInicio = inicio, fechaFin = fin)
        val tarea = original.toDto("uid", "obra-1").copy(id = "t1").toDomain()

        assertEquals("t1", tarea.id)
        assertEquals(original.titulo, tarea.titulo)
        assertEquals(original.descripcion, tarea.descripcion)
        assertEquals(CategoriaTarea.ELECTRICIDAD, tarea.categoria)
        assertEquals(EstadoTarea.EN_CURSO, tarea.estado)
        assertEquals(inicio, tarea.fechaInicio)
        assertEquals(fin, tarea.fechaFin)
    }

    @Test
    fun idaYVuelta_sinFechas_quedaSinFechas() {
        val tarea = draft().toDto("uid", "obra-1").toDomain()

        assertNull(tarea.fechaInicio)
        assertNull(tarea.fechaFin)
    }

    @Test
    fun toDomain_categoriaDesconocida_esOtros() {
        assertEquals(CategoriaTarea.OTROS, dto(categoria = "VOLADURAS").toDomain().categoria)
        assertEquals(CategoriaTarea.OTROS, dto(categoria = "").toDomain().categoria)
    }

    @Test
    fun toDomain_estadoDesconocido_esPendiente() {
        assertEquals(EstadoTarea.PENDIENTE, dto(estado = "ARCHIVADA").toDomain().estado)
        assertEquals(EstadoTarea.PENDIENTE, dto(estado = "").toDomain().estado)
    }

    @Test
    fun toDomain_fechaIsoInvalida_quedaSinFechas() {
        val tarea = dto(fechaInicio = "21/10/2026", fechaFin = "2026-10-30").toDomain()

        assertNull(tarea.fechaInicio)
        assertNull(tarea.fechaFin)
    }

    @Test
    fun toDomain_unaSolaFecha_quedaSinFechas() {
        val soloInicio = dto(fechaInicio = "2026-10-21").toDomain()
        val soloFin = dto(fechaFin = "2026-10-30").toDomain()

        assertNull(soloInicio.fechaInicio)
        assertNull(soloInicio.fechaFin)
        assertNull(soloFin.fechaInicio)
        assertNull(soloFin.fechaFin)
    }

    @Test
    fun toDomain_finAnteriorAInicio_quedaSinFechas() {
        val tarea = dto(fechaInicio = "2026-10-30", fechaFin = "2026-10-21").toDomain()

        assertNull(tarea.fechaInicio)
        assertNull(tarea.fechaFin)
    }

    @Test
    fun toDomain_finIgualAInicio_esValida() {
        val tarea = dto(fechaInicio = "2026-10-21", fechaFin = "2026-10-21").toDomain()

        assertEquals(inicio, tarea.fechaInicio)
        assertEquals(inicio, tarea.fechaFin)
    }

    @Test
    fun toTareaError_excepcionesPropias() {
        assertEquals(TareaError.NoEncontrada, TareaNotFoundException().toTareaError())
        assertEquals(TareaError.NoEncontrada, ObraNotFoundException().toTareaError())
        assertEquals(TareaError.NoAutorizada, TareaForbiddenException().toTareaError())
        assertEquals(TareaError.NoAutorizada, ObraForbiddenException().toTareaError())
    }

    @Test
    fun toTareaError_excepcionGenerica_esDesconocido() {
        assertEquals(TareaError.Desconocido, IllegalStateException("x").toTareaError())
    }

    private fun draft(fechaInicio: LocalDate? = null, fechaFin: LocalDate? = null) = TareaDraft(
        titulo = "Instalar tablero",
        descripcion = "Planta baja",
        categoria = CategoriaTarea.ELECTRICIDAD,
        fechaInicio = fechaInicio,
        fechaFin = fechaFin,
        estado = EstadoTarea.EN_CURSO
    )

    private fun dto(
        categoria: String = "ELECTRICIDAD",
        estado: String = "PENDIENTE",
        fechaInicio: String? = null,
        fechaFin: String? = null
    ) = TareaDto(
        id = "t1",
        obraId = "obra-1",
        userId = "uid",
        titulo = "Tarea",
        categoria = categoria,
        estado = estado,
        fechaInicio = fechaInicio,
        fechaFin = fechaFin
    )
}
