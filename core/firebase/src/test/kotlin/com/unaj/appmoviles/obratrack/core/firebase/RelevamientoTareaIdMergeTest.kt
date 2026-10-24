package com.unaj.appmoviles.obratrack.core.firebase

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Test

/** El update usa merge: sin tarea hay que borrar el campo, no omitirlo. */
class RelevamientoTareaIdMergeTest {

    @Test
    fun sinTarea_borraElCampo() {
        assertEquals(FieldValue.delete(), valorTareaIdParaMerge(null))
        assertEquals(FieldValue.delete(), valorTareaIdParaMerge(""))
    }

    @Test
    fun conTarea_escribeElId() {
        assertEquals("t1", valorTareaIdParaMerge("t1"))
    }

    @Test
    fun campoOpcionalQuitado_borraElCampo() {
        // Foto o próxima visita quitadas en el formulario.
        assertEquals(FieldValue.delete(), valorOpcionalParaMerge(null))
    }

    @Test
    fun campoOpcionalConValor_loEscribe() {
        assertEquals("https://foto", valorOpcionalParaMerge("https://foto"))
        assertEquals(1_700_000_000_000L, valorOpcionalParaMerge(1_700_000_000_000L))
    }

    @Test
    fun ubicacionQuitada_borraElCampo() {
        assertEquals(FieldValue.delete(), ubicacionParaMerge(null, null))
        // Media ubicación no se guarda (igual que en el alta).
        assertEquals(FieldValue.delete(), ubicacionParaMerge(-34.6, null))
    }

    @Test
    fun ubicacionConValor_seGuardaComoGeoPoint() {
        assertEquals(GeoPoint(-34.6, -58.4), ubicacionParaMerge(-34.6, -58.4))
    }
}
