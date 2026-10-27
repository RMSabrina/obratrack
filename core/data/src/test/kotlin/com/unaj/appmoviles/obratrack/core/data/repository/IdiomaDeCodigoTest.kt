package com.unaj.appmoviles.obratrack.core.data.repository

import com.unaj.appmoviles.obratrack.core.domain.model.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IdiomaDeCodigoTest {

    @Test
    fun reconoceLosTresIdiomas() {
        assertEquals(AppLanguage.SPANISH, idiomaDeCodigo("es"))
        assertEquals(AppLanguage.ENGLISH, idiomaDeCodigo("en"))
        assertEquals(AppLanguage.ITALIAN, idiomaDeCodigo("it"))
    }

    @Test
    fun ignoraLaRegionYLasMayusculas() {
        assertEquals(AppLanguage.SPANISH, idiomaDeCodigo("es-AR"))
        assertEquals(AppLanguage.ENGLISH, idiomaDeCodigo("EN"))
    }

    @Test
    fun codigoDesconocidoONuloDevuelveNull() {
        assertNull(idiomaDeCodigo("fr"))
        assertNull(idiomaDeCodigo(""))
        assertNull(idiomaDeCodigo(null))
    }
}
