package com.universopinol.store

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.universopinol.store.data.AppStatus
import com.universopinol.store.data.StoreApp
import com.universopinol.store.ui.DetailScreen
import com.universopinol.store.ui.HomeScreen
import com.universopinol.store.ui.StoreTheme
import com.universopinol.store.ui.StoreViewModel
import com.universopinol.store.ui.UP
import com.universopinol.store.work.UpdateWorker

class MainActivity : ComponentActivity() {

    private val vm: StoreViewModel by viewModels()
    private val askNotifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        UpdateWorker.schedule(this)
        if (Build.VERSION.SDK_INT >= 33 && savedInstanceState == null) askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        setContent { StoreTheme { App() } }
    }

    override fun onResume() {
        super.onResume()
        vm.refreshInstalled()
        vm.resumeAfterPermission()
    }

    private fun act(app: StoreApp, status: AppStatus) {
        if (status == AppStatus.INSTALLED) vm.openIntent(app.id)?.let { startActivity(it) } else vm.install(app)
    }

    @androidx.compose.runtime.Composable
    private fun App() {
        val s by vm.state.collectAsStateWithLifecycle()
        var detailId by rememberSaveable { mutableStateOf<String?>(null) }
        val snack = remember { SnackbarHostState() }

        LaunchedEffect(s.message) {
            s.message?.let { snack.showSnackbar(it); vm.consumeMessage() }
        }

        Scaffold(containerColor = UP.Bg, snackbarHost = { SnackbarHost(snack) }) { pad ->
            Box(Modifier.fillMaxSize().padding(pad)) {
                val detail = detailId?.let { id -> s.catalog.apps.firstOrNull { it.id == id } }
                if (detail != null) {
                    BackHandler { detailId = null }
                    DetailScreen(
                        app = detail, s = s, installedName = vm.installedVersionName(detail.id),
                        onBack = { detailId = null },
                        onAction = { st -> act(detail, st) },
                        onUninstall = { startActivity(vm.uninstallIntent(detail.id)) }
                    )
                } else {
                    HomeScreen(
                        s = s,
                        onRefresh = vm::refresh,
                        onOpenDetail = { detailId = it.id },
                        onAction = ::act,
                        onUpdateAll = vm::updateAll
                    )
                }
            }
        }

        s.needsPermissionFor?.let { app ->
            AlertDialog(
                onDismissRequest = vm::permissionHandled,
                title = { Text("Permiso para instalar") },
                text = {
                    Text(
                        "Para instalar ${app.name}, Android necesita que permitas a Universo Pinol Store instalar apps. " +
                            "En la siguiente pantalla activa «Permitir de esta fuente» y vuelve con la flecha atrás. Solo se hace una vez."
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        vm.goToPermission()
                        startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName")))
                    }) { Text("Dar permiso") }
                },
                dismissButton = { TextButton(onClick = vm::permissionHandled) { Text("Ahora no") } }
            )
        }
    }
}
