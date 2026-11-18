package com.universopinol.store.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.universopinol.store.data.Catalog
import com.universopinol.store.data.Repository
import com.universopinol.store.data.Store
import com.universopinol.store.data.StoreApp
import com.universopinol.store.install.Downloader
import com.universopinol.store.install.InstallEvents
import com.universopinol.store.install.Handoff
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface Job {
    data class Downloading(val progress: Float) : Job
    data object Installing : Job
}

data class UiState(
    val loading: Boolean = true,
    val offline: Boolean = false,
    val catalog: Catalog = Catalog(),
    val installed: Map<String, Long> = emptyMap(),
    val jobs: Map<String, Job> = emptyMap(),
    val message: String? = null,
    /** App ya descargada y verificada, lista para que el usuario la instale desde «Descargas». */
    val readyApp: StoreApp? = null
) {
    val apps: List<StoreApp> get() = catalog.apps.filter { it.visible && it.id != Store.SELF_ID }
    val selfUpdate: StoreApp? get() = catalog.apps.firstOrNull { it.id == Store.SELF_ID }?.takeIf { (installed[it.id] ?: Long.MAX_VALUE) < it.versionCode }
    val updates: List<StoreApp> get() = Store.updates(catalog, installed).filter { it.id != Store.SELF_ID }
}

class StoreViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = Repository(app)
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        repo.cached()?.let { c -> _state.update { it.copy(catalog = c, installed = repo.installed(c), loading = false) } }
        refresh()
        viewModelScope.launch {
            InstallEvents.events.collect { r ->
                _state.update { it.copy(jobs = it.jobs - r.packageName, message = if (r.ok) null else r.message) }
                refreshInstalled()
                if (r.ok) {
                    val name = _state.value.catalog.apps.firstOrNull { it.id == r.packageName }?.name ?: "La app"
                    _state.update { it.copy(message = "$name quedó instalada.") }
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            val result = withContext(Dispatchers.IO) { runCatching { repo.fetch() } }
            result.onSuccess { c ->
                val inst = withContext(Dispatchers.IO) { repo.installed(c) }
                _state.update { it.copy(catalog = c, installed = inst, loading = false, offline = false) }
            }.onFailure {
                _state.update { it.copy(loading = false, offline = true) }
            }
        }
    }

    /** Al volver a la tienda (después de instalar, desinstalar o dar permisos). */
    fun refreshInstalled() {
        val c = _state.value.catalog
        viewModelScope.launch {
            val inst = withContext(Dispatchers.IO) { repo.installed(c) }
            _state.update { s ->
                // Si Android cerró el diálogo de instalación sin avisar, se libera el botón.
                val jobs = s.jobs.filterKeys { id -> s.jobs[id] !is Job.Installing || (inst[id] ?: -1) < (c.apps.firstOrNull { it.id == id }?.versionCode ?: 0) }
                s.copy(installed = inst, jobs = jobs)
            }
        }
    }

    fun installedVersionName(id: String): String? = repo.installedVersionName(id)

    fun install(app: StoreApp) {
        val ctx = getApplication<Application>()
        if (_state.value.jobs.containsKey(app.id)) return
        _state.update { it.copy(jobs = it.jobs + (app.id to Job.Downloading(0f)), message = null) }
        viewModelScope.launch {
            val r = withContext(Dispatchers.IO) {
                runCatching {
                    val file = Downloader.download(ctx, app) { p -> _state.update { it.copy(jobs = it.jobs + (app.id to Job.Downloading(p))) } }
                    Handoff.saveToDownloads(ctx, file, "${app.name} ${app.versionName}.apk")
                    file.delete()
                }
            }
            r.onSuccess { _state.update { it.copy(jobs = it.jobs - app.id, readyApp = app) } }
            r.onFailure { e ->
                _state.update { it.copy(jobs = it.jobs - app.id, message = e.message ?: "No se pudo descargar. Revisa tu internet.") }
            }
        }
    }

    fun updateAll() = _state.value.updates.forEach { install(it) }

    fun readyHandled() = _state.update { it.copy(readyApp = null) }

    fun consumeMessage() = _state.update { it.copy(message = null) }

    fun openIntent(id: String): Intent? = getApplication<Application>().packageManager.getLaunchIntentForPackage(id)

    fun uninstallIntent(id: String): Intent = Intent(Intent.ACTION_DELETE, Uri.parse("package:$id"))
}
