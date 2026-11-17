package com.universopinol.store

import com.universopinol.store.data.AppStatus
import com.universopinol.store.data.Store
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class CatalogTest {

    private val text = """
        {"updatedAt":"2026-11-16","apps":[
          {"id":"com.lecolite.app","name":"Leco Lite","versionCode":8,"versionName":"1.0.8",
           "apkUrl":"https://x/LecoLite.apk","icon":"icons/lecolite.png","size":12400000,"campoNuevo":true},
          {"id":"com.oculta","name":"Oculta","versionCode":2,"versionName":"1.0.2","apkUrl":"https://x/o.apk","visible":false}
        ]}
    """.trimIndent()

    @Test fun leeElCatalogoAunqueTengaCamposNuevos() {
        val c = Store.parse(text)
        assertEquals(2, c.apps.size)
        assertEquals("Leco Lite", c.apps[0].name)
        assertEquals(Store.RAW + "icons/lecolite.png", Store.iconUrl(c.apps[0]))
        assertEquals("12.4 MB", Store.size(c.apps[0].size))
    }

    @Test fun estadoDeCadaApp() {
        val app = Store.parse(text).apps[0]
        assertEquals(AppStatus.NOT_INSTALLED, Store.status(app, null))
        assertEquals(AppStatus.UPDATE, Store.status(app, 7))
        assertEquals(AppStatus.INSTALLED, Store.status(app, 8))
        assertEquals(AppStatus.INSTALLED, Store.status(app, 9))
    }

    @Test fun actualizacionesSinContarOcultas() {
        val c = Store.parse(text)
        val ups = Store.updates(c, mapOf("com.lecolite.app" to 5L, "com.oculta" to 1L))
        assertEquals(listOf("com.lecolite.app"), ups.map { it.id })
    }

    /** El catalog.json real del repositorio debe leerse sin errores. */
    @Test fun catalogoRealValido() {
        val f = listOf(File("../catalog.json"), File("catalog.json")).first { it.exists() }
        val c = Store.parse(f.readText())
        assertTrue(c.apps.isNotEmpty())
        c.apps.forEach { a ->
            assertTrue(a.id, a.apkUrl.startsWith("https://"))
            assertTrue(a.id, a.versionCode > 0)
        }
    }
}
