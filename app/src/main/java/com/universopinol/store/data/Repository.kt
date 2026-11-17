package com.universopinol.store.data

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.pm.PackageInfoCompat
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** Descarga el catálogo (con copia guardada para abrir sin internet) y lee qué apps están instaladas. */
class Repository(private val context: Context) {

    private val cacheFile = File(context.filesDir, "catalog.json")

    fun cached(): Catalog? = runCatching { Store.parse(cacheFile.readText()) }.getOrNull()

    /** Baja el catálogo más reciente. Lanza una excepción si no hay internet. */
    fun fetch(): Catalog {
        val text = httpText(Store.CATALOG_URL + "?t=" + System.currentTimeMillis() / 60_000)
        val catalog = Store.parse(text)
        runCatching { cacheFile.writeText(text) }
        return catalog
    }

    fun installedVersion(id: String): Long? = try {
        PackageInfoCompat.getLongVersionCode(context.packageManager.getPackageInfo(id, 0))
    } catch (e: PackageManager.NameNotFoundException) {
        null
    }

    fun installedVersionName(id: String): String? = try {
        context.packageManager.getPackageInfo(id, 0).versionName
    } catch (e: PackageManager.NameNotFoundException) {
        null
    }

    fun installed(c: Catalog): Map<String, Long> = c.apps.mapNotNull { a -> installedVersion(a.id)?.let { a.id to it } }.toMap()

    companion object {
        fun httpText(url: String): String {
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000; readTimeout = 20_000
                setRequestProperty("Cache-Control", "no-cache")
            }
            try {
                if (conn.responseCode !in 200..299) error("El servidor respondió ${conn.responseCode}")
                return conn.inputStream.bufferedReader().use { it.readText() }
            } finally {
                conn.disconnect()
            }
        }
    }
}
