package com.universopinol.store.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Una app de la tienda, tal como viene en catalog.json. */
@Serializable
data class StoreApp(
    /** Nombre del paquete Android, p. ej. com.lecolite.app */
    val id: String,
    val name: String,
    val tagline: String = "",
    val description: String = "",
    val category: String = "",
    /** Color de respaldo para el ícono, en #RRGGBB */
    val color: String = "#8B5CF6",
    /** Ruta dentro del repositorio (icons/x.png) o URL completa. */
    val icon: String = "",
    val versionCode: Long,
    val versionName: String,
    val apkUrl: String,
    val sha256: String = "",
    val size: Long = 0,
    val changelog: String = "",
    val updated: String = "",
    /** false para ocultarla sin borrarla del catálogo. */
    val visible: Boolean = true
)

@Serializable
data class Catalog(val updatedAt: String = "", val apps: List<StoreApp> = emptyList())

enum class AppStatus { NOT_INSTALLED, UPDATE, INSTALLED }

object Store {
    const val OWNER = "skylan2684"
    const val REPO = "UniversoPinolStore"
    const val RAW = "https://raw.githubusercontent.com/$OWNER/$REPO/main/"
    const val CATALOG_URL = RAW + "catalog.json"
    const val SELF_ID = "com.universopinol.store"

    val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun parse(text: String): Catalog = json.decodeFromString(Catalog.serializer(), text)

    fun status(app: StoreApp, installedVersion: Long?): AppStatus = when {
        installedVersion == null -> AppStatus.NOT_INSTALLED
        installedVersion < app.versionCode -> AppStatus.UPDATE
        else -> AppStatus.INSTALLED
    }

    /** Apps con actualización disponible, sin contar las ocultas. */
    fun updates(c: Catalog, installed: Map<String, Long>): List<StoreApp> =
        c.apps.filter { it.visible && status(it, installed[it.id]) == AppStatus.UPDATE }

    fun iconUrl(app: StoreApp): String? = app.icon.takeIf { it.isNotBlank() }?.let { if (it.startsWith("http")) it else RAW + it.trimStart('/') }

    /** "12.4 MB" */
    fun size(bytes: Long): String = when {
        bytes <= 0 -> ""
        bytes < 1_000_000 -> "${(bytes + 999) / 1000} KB"
        else -> "%.1f MB".format(java.util.Locale.US, bytes / 1_000_000.0)
    }

    fun parseColor(hex: String): Long = runCatching { 0xFF000000 or hex.trim().removePrefix("#").toLong(16) }.getOrDefault(0xFF8B5CF6)
}
