package com.universopinol.store.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.universopinol.store.data.AppStatus
import com.universopinol.store.data.Store
import com.universopinol.store.data.StoreApp

// ───────────────────────────── Inicio ─────────────────────────────

@Composable
fun HomeScreen(
    s: UiState,
    onRefresh: () -> Unit,
    onOpenDetail: (StoreApp) -> Unit,
    onAction: (StoreApp, AppStatus) -> Unit,
    onUpdateAll: () -> Unit,
    mode: ThemeMode = ThemeMode.AUTO,
    onToggleTheme: () -> Unit = {}
) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
        item(key = "cabecera") { Header(s.loading, onRefresh, mode, onToggleTheme) }

        if (s.offline) item(key = "offline") {
            Banner(Icons.Filled.CloudOff, "Sin conexión", "Mostrando el último catálogo guardado. Toca ↻ para reintentar.", UP.Amber)
        }

        s.selfUpdate?.let { self ->
            item(key = "tienda") {
                Card(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), border = UP.VioletLight.copy(alpha = 0.5f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.SystemUpdate, null, tint = UP.VioletLight)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Nueva versión de la tienda", fontWeight = FontWeight.SemiBold)
                            Text("Universo Pinol Store ${self.versionName}", color = UP.Muted, fontSize = 13.sp)
                        }
                        ActionButton(self, AppStatus.UPDATE, s.jobs[self.id], compact = true) { onAction(self, AppStatus.UPDATE) }
                    }
                }
            }
        }

        if (s.updates.isNotEmpty()) item(key = "actualizaciones") {
            Card(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), border = UP.Amber.copy(alpha = 0.45f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (s.updates.size == 1) "1 actualización disponible" else "${s.updates.size} actualizaciones disponibles",
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(s.updates.joinToString(", ") { it.name }, color = UP.Muted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    if (s.updates.size > 1) Button(onClick = onUpdateAll) { Text("Actualizar todo") }
                }
            }
        }

        val destacadas = s.apps.takeLast(2).reversed()
        if (destacadas.isNotEmpty()) item(key = "destacadas") {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(destacadas, key = { it.id }) { app ->
                    val status = Store.status(app, s.installed[app.id])
                    FeaturedCard(app, status, s.jobs[app.id], { onOpenDetail(app) }) { onAction(app, status) }
                }
            }
        }

        item(key = "titulo") {
            Text("Todas las apps", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 20.dp, top = 14.dp, bottom = 6.dp))
        }

        if (s.apps.isEmpty() && !s.loading) item(key = "vacio") {
            Text(
                if (s.offline) "Conéctate a internet para ver las apps." else "Todavía no hay apps publicadas.",
                color = UP.Muted, modifier = Modifier.padding(20.dp)
            )
        }

        items(s.apps, key = { it.id }) { app ->
            val status = Store.status(app, s.installed[app.id])
            AppRow(app, status, s.jobs[app.id], { onOpenDetail(app) }) { onAction(app, status) }
        }
    }
}

@Composable
private fun Header(loading: Boolean, onRefresh: () -> Unit, mode: ThemeMode, onToggleTheme: () -> Unit) {
    Box(
        Modifier.fillMaxWidth()
            .background(Brush.verticalGradient(listOf(UP.BgTop, UP.Bg)))
            .padding(start = 20.dp, end = 8.dp, top = 24.dp, bottom = 18.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text("Universo Pinol", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = UP.VioletLight)
                Text("Descubre", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = UP.Text, modifier = Modifier.padding(top = 6.dp))
                Text("el universo", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = UP.Text)
                Text("Apps hechas por y para tu equipo.", color = UP.Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
            }
            IconButton(onClick = onToggleTheme) {
                val (icono, texto) = when (mode) {
                    ThemeMode.AUTO -> Icons.Filled.BrightnessAuto to "Tema automático. Toca para cambiar"
                    ThemeMode.LIGHT -> Icons.Filled.LightMode to "Tema claro. Toca para cambiar"
                    ThemeMode.DARK -> Icons.Filled.DarkMode to "Tema oscuro. Toca para cambiar"
                }
                Icon(icono, texto, tint = UP.Text)
            }
            if (loading) CircularProgressIndicator(Modifier.size(28.dp).padding(4.dp), strokeWidth = 3.dp, color = UP.VioletLight)
            else IconButton(onClick = onRefresh) { Icon(Icons.Filled.Refresh, "Buscar actualizaciones", tint = UP.Text) }
        }
    }
}

@Composable
private fun FeaturedCard(app: StoreApp, status: AppStatus, job: Job?, onClick: () -> Unit, onAction: () -> Unit) {
    val base = Color(Store.parseColor(app.color))
    val shape = RoundedCornerShape(26.dp)
    OnDarkSurface {
        Column(
            Modifier.width(250.dp).height(210.dp).clip(shape)
                .background(Brush.linearGradient(listOf(lerp(base, Color.Black, 0.5f), lerp(base, Color.Black, 0.75f))))
                .border(1.dp, Color.White.copy(alpha = 0.12f), shape)
                .clickable(onClick = onClick).padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            AppIcon(app, status != AppStatus.NOT_INSTALLED, 60.dp)
            Column {
                Text(app.name, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    app.tagline, fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f), maxLines = 2, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
                )
                ActionButton(app, status, job, compact = true, onClick = onAction)
            }
        }
    }
}

@Composable
private fun AppRow(app: StoreApp, status: AppStatus, job: Job?, onClick: () -> Unit, onAction: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp)
            .clip(shape).background(UP.Card).border(1.dp, UP.Border, shape)
            .clickable(onClick = onClick).padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppIcon(app, status != AppStatus.NOT_INSTALLED, 56.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(app.name, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (app.tagline.isNotBlank()) Text(app.tagline, color = UP.Muted, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    listOf(app.category, "v${app.versionName}", Store.size(app.size)).filter { it.isNotBlank() }.joinToString(" · "),
                    color = UP.Muted, fontSize = 12.sp
                )
            }
            Spacer(Modifier.width(8.dp))
            ActionButton(app, status, job, compact = true, onClick = onAction)
        }
        JobProgress(job)
    }
}

@Composable
private fun JobProgress(job: Job?) {
    when (job) {
        is Job.Downloading -> {
            LinearProgressIndicator(
                progress = { job.progress },
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp).height(5.dp).clip(RoundedCornerShape(3.dp)),
                color = UP.VioletLight, trackColor = UP.Border
            )
            Text("Descargando ${(job.progress * 100).toInt()} %", color = UP.Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
        }
        Job.Installing -> {
            LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 10.dp).height(5.dp).clip(RoundedCornerShape(3.dp)), color = UP.Green, trackColor = UP.Border)
            Text("Instalando… confirma en la ventana de Android si aparece", color = UP.Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
        }
        null -> {}
    }
}

@Composable
fun ActionButton(app: StoreApp, status: AppStatus, job: Job?, compact: Boolean = false, onClick: () -> Unit) {
    val mod = if (compact) Modifier else Modifier.fillMaxWidth().height(50.dp)
    when {
        job != null -> OutlinedButton(onClick = {}, enabled = false, modifier = mod) { Text(if (job is Job.Installing) "Instalando" else "Bajando") }
        status == AppStatus.INSTALLED -> OutlinedButton(onClick = onClick, modifier = mod, border = BorderStroke(1.dp, UP.Green)) { Text("Abrir", color = UP.Green) }
        status == AppStatus.UPDATE -> Button(onClick = onClick, modifier = mod, colors = ButtonDefaults.buttonColors(containerColor = UP.Amber, contentColor = UP.Bg)) { Text("Actualizar") }
        else -> Button(onClick = onClick, modifier = mod) { Text("Instalar") }
    }
}

// ───────────────────────────── Detalle ─────────────────────────────

@Composable
fun DetailScreen(
    app: StoreApp,
    s: UiState,
    installedName: String?,
    onBack: () -> Unit,
    onAction: (AppStatus) -> Unit,
    onUninstall: () -> Unit
) {
    val status = Store.status(app, s.installed[app.id])
    val job = s.jobs[app.id]
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        IconButton(onClick = onBack, modifier = Modifier.padding(4.dp)) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") }
        Column(Modifier.padding(horizontal = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIcon(app, status != AppStatus.NOT_INSTALLED, 84.dp)
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(app.name, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    if (app.category.isNotBlank()) Text(app.category, color = UP.VioletLight)
                    Text("Universo Pinol", color = UP.Muted, fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(18.dp))
            ActionButton(app, status, job) { onAction(status) }
            JobProgress(job)
            if (status == AppStatus.UPDATE && job == null) {
                Text("Tienes la versión ${installedName ?: "anterior"}. Se actualiza encima y conservas tus datos.", color = UP.Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
            }

            if (app.changelog.isNotBlank()) {
                Section("Novedades")
                Text(app.changelog, lineHeight = 21.sp)
            }
            if (app.description.isNotBlank()) {
                Section("Acerca de esta app")
                Text(app.description, lineHeight = 21.sp)
            }

            Section("Información")
            Info("Versión", app.versionName)
            if (installedName != null) Info("Instalada", installedName)
            if (app.size > 0) Info("Tamaño", Store.size(app.size))
            if (app.updated.isNotBlank()) Info("Actualizada", app.updated)
            Info("Paquete", app.id)

            if (status != AppStatus.NOT_INSTALLED && app.id != Store.SELF_ID) {
                TextButton(onClick = onUninstall, modifier = Modifier.padding(top = 12.dp)) { Text("Desinstalar", color = UP.Red) }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun Section(title: String) {
    Text(title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 24.dp, bottom = 8.dp))
}

@Composable
private fun Info(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(label, color = UP.Muted, modifier = Modifier.weight(1f))
        Text(value)
    }
    HorizontalDivider(color = UP.Border)
}

// ───────────────────────────── Piezas ─────────────────────────────

@Composable
private fun Card(modifier: Modifier = Modifier, border: androidx.compose.ui.graphics.Color = UP.Border, content: @Composable () -> Unit) {
    Box(
        modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(UP.Card)
            .border(1.dp, border, RoundedCornerShape(18.dp)).padding(16.dp)
    ) { content() }
}

@Composable
private fun Banner(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, body: String, color: androidx.compose.ui.graphics.Color) {
    Card(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), border = color.copy(alpha = 0.45f)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, null, tint = color)
            Column {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(body, color = UP.Muted, fontSize = 13.sp)
            }
        }
    }
}
