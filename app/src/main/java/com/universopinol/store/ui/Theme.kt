package com.universopinol.store.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object UP {
    val Bg = Color(0xFF070B16)
    val Card = Color(0xFF0E1426)
    val CardHigh = Color(0xFF151D33)
    val Border = Color(0xFF232D47)
    val Violet = Color(0xFF8B5CF6)
    val VioletLight = Color(0xFFA78BFA)
    val Green = Color(0xFF34D399)
    val Amber = Color(0xFFFBBF24)
    val Blue = Color(0xFF38BDF8)
    val Red = Color(0xFFF87171)
    val Text = Color(0xFFF1F5F9)
    val Muted = Color(0xFF94A3B8)
}

private val scheme = darkColorScheme(
    primary = UP.Violet,
    onPrimary = Color.White,
    secondary = UP.Blue,
    tertiary = UP.Green,
    background = UP.Bg,
    onBackground = UP.Text,
    surface = UP.Bg,
    onSurface = UP.Text,
    surfaceVariant = UP.Card,
    onSurfaceVariant = UP.Muted,
    surfaceContainer = UP.Card,
    surfaceContainerHigh = UP.CardHigh,
    surfaceContainerHighest = UP.CardHigh,
    surfaceContainerLow = UP.Card,
    outline = UP.Border,
    error = UP.Red
)

@Composable
fun StoreTheme(content: @Composable () -> Unit) = MaterialTheme(colorScheme = scheme, content = content)
