package com.universopinol.store.install

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import java.io.File

/** Resultado de una instalación, para que la pantalla se entere. */
data class InstallResult(val packageName: String, val ok: Boolean, val message: String = "")

object InstallEvents {
    private val _events = MutableSharedFlow<InstallResult>(extraBufferCapacity = 8)
    val events: SharedFlow<InstallResult> = _events
    fun emit(r: InstallResult) { _events.tryEmit(r) }
}

object Installer {

    /** ¿Android ya le dio permiso a la tienda para instalar apps? */
    fun canInstall(context: Context): Boolean = context.packageManager.canRequestPackageInstalls()

    /**
     * Comprueba que la APK es de verdad la app esperada y la instala con el instalador de Android.
     * Android pide confirmación la primera vez; las actualizaciones posteriores pueden ir directas.
     */
    fun install(context: Context, apk: File, expectedPackage: String) {
        val info = context.packageManager.getPackageArchiveInfo(apk.absolutePath, 0)
            ?: error("El archivo descargado no es una app válida.")
        if (info.packageName != expectedPackage) error("La app descargada no coincide (${info.packageName}).")

        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(expectedPackage)
            setSize(apk.length())
            if (Build.VERSION.SDK_INT >= 31) setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
        }
        val sessionId = installer.createSession(params)
        installer.openSession(sessionId).use { session ->
            apk.inputStream().use { input ->
                session.openWrite("base.apk", 0, apk.length()).use { out ->
                    input.copyTo(out)
                    session.fsync(out)
                }
            }
            val intent = Intent(context, InstallResultReceiver::class.java).putExtra(EXTRA_PKG, expectedPackage)
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0)
            val pending = PendingIntent.getBroadcast(context, sessionId, intent, flags)
            session.commit(pending.intentSender)
        }
    }

    const val EXTRA_PKG = "store_pkg"
}

class InstallResultReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pkg = intent.getStringExtra(Installer.EXTRA_PKG) ?: ""
        when (val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                @Suppress("DEPRECATION")
                val confirm = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
                if (confirm != null) context.startActivity(confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            PackageInstaller.STATUS_SUCCESS -> InstallEvents.emit(InstallResult(pkg, true))
            else -> {
                val msg = when (status) {
                    PackageInstaller.STATUS_FAILURE_ABORTED -> "Instalación cancelada."
                    PackageInstaller.STATUS_FAILURE_STORAGE -> "No hay espacio suficiente en el teléfono."
                    PackageInstaller.STATUS_FAILURE_CONFLICT ->
                        "La app instalada tiene otra firma. Desinstálala primero (se pierden sus datos) y vuelve a instalarla."
                    PackageInstaller.STATUS_FAILURE_INCOMPATIBLE -> "Esta app no es compatible con este teléfono."
                    PackageInstaller.STATUS_FAILURE_BLOCKED -> "Android bloqueó la instalación (revisa Play Protect)."
                    else -> intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE) ?: "No se pudo instalar."
                }
                InstallEvents.emit(InstallResult(pkg, false, msg))
            }
        }
    }
}
