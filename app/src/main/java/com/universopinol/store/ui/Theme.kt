package com.universopinol.store.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/** Automático sigue al teléfono; Claro y Oscuro lo fija la persona desde el botón de la cabecera. */
enum class ThemeMode { AUTO, LIGHT, DARK }

@Immutable
class Palette(
    val dark: Boolean,
    val bg: Color, val bgTop: Color, val card: Color, val cardHigh: Color, val border: Color,
    val violet: Color, val violetLight: Color, val accent: Color, val onAccent: Color,
    val green: Color, val amber: Color, val blue: Color, val red: Color,
    val text: Color, val muted: Color
)

private val DarkPalette = Palette(
    dark = true,
    bg = Color(0xFF0B0F26), bgTop = Color(0xFF2A1B5E), card = Color(0xFF171A38), cardHigh = Color(0xFF1F2347), border = Color(0xFF2A2F55),
    violet = Color(0xFF8B5CF6), violetLight = Color(0xFFA78BFA), accent = Color(0xFFD9F27B), onAccent = Color(0xFF14262A),
    green = Color(0xFF34D399), amber = Color(0xFFFBBF24), blue = Color(0xFF38BDF8), red = Color(0xFFF87171),
    text = Color(0xFFF4F4FB), muted = Color(0xFFA9A6D6)
)

private val LightPalette = Palette(
    dark = false,
    bg = Color(0xFFF5F3EC), bgTop = Color(0xFFE4DCFA), card = Color(0xFFFFFFFF), cardHigh = Color(0xFFEFEBDD), border = Color(0xFFE3DFD2),
    violet = Color(0xFF6D3FE0), violetLight = Color(0xFF6D3FE0), accent = Color(0xFFD9F27B), onAccent = Color(0xFF14262A),
    green = Color(0xFF15803D), amber = Color(0xFFB45309), blue = Color(0xFF0369A1), red = Color(0xFFDC2626),
    text = Color(0xFF14262A), muted = Color(0xFF5B6B6E)
)

val LocalPalette = compositionLocalOf { DarkPalette }

/** Colores de la tienda; siguen al tema activo (claro u oscuro). */
object UP {
    val Bg: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.bg
    val BgTop: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.bgTop
    val Card: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.card
    val CardHigh: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.cardHigh
    val Border: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.border
    val Violet: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.violet
    val VioletLight: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.violetLight
    val Green: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.green
    val Amber: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.amber
    val Blue: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.blue
    val Red: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.red
    val Text: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.text
    val Muted: Color @Composable @ReadOnlyComposable get() = LocalPalette.current.muted
}

private fun schemeFor(p: Palette): ColorScheme {
    return if (p.dark) darkColorScheme(
        primary = p.accent, onPrimary = p.onAccent, secondary = p.blue, tertiary = p.green,
        background = p.bg, onBackground = p.text, surface = p.bg, onSurface = p.text,
        surfaceVariant = p.card, onSurfaceVariant = p.muted,
        surfaceContainer = p.card, surfaceContainerHigh = p.cardHigh, surfaceContainerHighest = p.cardHigh, surfaceContainerLow = p.card,
        outline = p.border, error = p.red
    ) else lightColorScheme(
        primary = p.accent, onPrimary = p.onAccent, secondary = p.blue, tertiary = p.green,
        background = p.bg, onBackground = p.text, surface = p.bg, onSurface = p.text,
        surfaceVariant = p.card, onSurfaceVariant = p.muted,
        surfaceContainer = p.card, surfaceContainerHigh = p.cardHigh, surfaceContainerHighest = p.cardHigh, surfaceContainerLow = p.card,
        outline = p.border, error = p.red
    )
}

@Composable
fun StoreTheme(mode: ThemeMode = ThemeMode.AUTO, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.AUTO -> isSystemInDarkTheme()
    }
    val palette = if (dark) DarkPalette else LightPalette
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as android.app.Activity).window
            window.statusBarColor = palette.bg.toArgb()
            window.navigationBarColor = palette.bg.toArgb()
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !dark
            controller.isAppearanceLightNavigationBars = !dark
        }
    }
    CompositionLocalProvider(LocalPalette provides palette) {
        MaterialTheme(colorScheme = schemeFor(palette), content = content)
    }
}

/** Para tarjetas de color que siempre son oscuras (los destacados), también en modo claro. */
@Composable
fun OnDarkSurface(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalPalette provides DarkPalette) {
        MaterialTheme(colorScheme = schemeFor(DarkPalette), content = content)
    }
}
