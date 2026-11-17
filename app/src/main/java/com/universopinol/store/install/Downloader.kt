package com.universopinol.store.install

import android.content.Context
import com.universopinol.store.data.StoreApp
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/** Descarga la APK con su progreso y comprueba que llegó completa y sin cambios (SHA-256). */
object Downloader {

    fun download(context: Context, app: StoreApp, onProgress: (Float) -> Unit): File {
        val dir = File(context.cacheDir, "apks").apply { mkdirs() }
        dir.listFiles()?.forEach { if (it.name.startsWith(app.id + "-")) it.delete() }
        val out = File(dir, "${app.id}-${app.versionCode}.apk")
        val tmp = File(dir, out.name + ".part")

        var url = URL(app.apkUrl)
        var conn: HttpURLConnection
        var hops = 0
        while (true) {
            conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 20_000; readTimeout = 30_000; instanceFollowRedirects = false
            }
            val code = conn.responseCode
            if (code in 300..399 && hops < 6) {
                val next = conn.getHeaderField("Location") ?: error("Redirección sin destino")
                conn.disconnect(); url = URL(url, next); hops++; continue
            }
            if (code !in 200..299) { conn.disconnect(); error("No se pudo descargar (código $code)") }
            break
        }

        val total = conn.contentLengthLong.takeIf { it > 0 } ?: app.size
        val digest = MessageDigest.getInstance("SHA-256")
        try {
            conn.inputStream.use { input ->
                tmp.outputStream().use { output ->
                    val buf = ByteArray(64 * 1024)
                    var done = 0L
                    var last = 0L
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        output.write(buf, 0, n)
                        digest.update(buf, 0, n)
                        done += n
                        if (total > 0 && done - last > 128 * 1024) { last = done; onProgress((done.toFloat() / total).coerceIn(0f, 1f)) }
                    }
                }
            }
        } finally {
            conn.disconnect()
        }
        val hash = digest.digest().joinToString("") { "%02x".format(it) }
        if (app.sha256.isNotBlank() && !hash.equals(app.sha256.trim(), ignoreCase = true)) {
            tmp.delete()
            error("La descarga llegó dañada. Inténtalo de nuevo.")
        }
        if (!tmp.renameTo(out)) { tmp.copyTo(out, overwrite = true); tmp.delete() }
        onProgress(1f)
        return out
    }
}
