package com.unaj.appmoviles.obratrack.core.firebase

import com.google.firebase.firestore.FieldValue
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
}
