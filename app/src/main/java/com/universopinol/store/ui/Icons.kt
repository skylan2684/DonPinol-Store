package com.universopinol.store.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.universopinol.store.data.Store
import com.universopinol.store.data.StoreApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

private object IconCache {
    val map = java.util.concurrent.ConcurrentHashMap<String, ImageBitmap>()
}

/** Ícono de la app: el real si está instalada, si no el del catálogo, y si no la inicial con su color. */
@Composable
fun AppIcon(app: StoreApp, installed: Boolean, size: Dp) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(size * 0.24f)
    val key = app.id + if (installed) ":i" else ":r"
    val img by produceState(IconCache.map[key], key) {
        if (value != null) return@produceState
        value = withContext(Dispatchers.IO) {
            runCatching {
                val bmp: ImageBitmap? = if (installed) {
                    val d: Drawable = context.packageManager.getApplicationIcon(app.id)
                    d.toBitmap(192, 192).asImageBitmap()
                } else Store.iconUrl(app)?.let { url ->
                    URL(url).openStream().use { BitmapFactory.decodeStream(it) as Bitmap? }?.asImageBitmap()
                }
                bmp
            }.getOrNull()?.also { IconCache.map[key] = it }
        }
    }
    val bmp = img
    if (bmp != null) {
        Image(bmp, contentDescription = app.name, modifier = Modifier.size(size).clip(shape))
    } else {
        Box(Modifier.size(size).clip(shape).background(Color(Store.parseColor(app.color))), contentAlignment = Alignment.Center) {
            Text(app.name.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = (size.value * 0.42f).sp)
        }
    }
}
