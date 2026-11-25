package com.universopinol.store.install

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import androidx.core.content.FileProvider
import java.io.File

/** Resultado de una instalación, para que la pantalla se entere. */
data class InstallResult(val packageName: String, val ok: Boolean, val message: String = "")

object InstallEvents {
    private val _events = MutableSharedFlow<InstallResult>(extraBufferCapacity = 8)
    val events: SharedFlow<InstallResult> = _events
    fun emit(r: InstallResult) { _events.tryEmit(r) }
}

/**
 * La tienda NO instala apps por su cuenta: descarga y verifica la APK (SHA-256) y la deja en «Descargas».
 * La instalación la hace el usuario con el instalador de Android, igual que con cualquier archivo descargado.
 */
object Handoff {
    /** Guarda la APK ya verificada en la carpeta Descargas (no necesita permisos de almacenamiento). */
    fun saveToDownloads(context: Context, apk: File, displayName: String): Uri? {
        // Android 8 y 9 (muchas radios de carro) no tienen la carpeta Descargas por MediaStore:
        // se devuelve un enlace al archivo ya verificado y la pantalla abre el instalador de Android directo.
        if (Build.VERSION.SDK_INT < 29) {
            return FileProvider.getUriForFile(context, context.packageName + ".apkprovider", apk)
        }
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, displayName)
            put(MediaStore.Downloads.MIME_TYPE, "application/vnd.android.package-archive")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: error("No se pudo guardar en Descargas.")
        try {
            val out = resolver.openOutputStream(uri) ?: error("No se pudo guardar en Descargas.")
            out.use { o -> apk.inputStream().use { it.copyTo(o) } }
            val done = ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }
            resolver.update(uri, done, null, null)
            return null
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            throw e
        }
    }
}
