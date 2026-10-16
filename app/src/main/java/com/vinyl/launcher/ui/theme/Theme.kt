package com.vinyl.launcher.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontFamily
import com.vinyl.launcher.data.Anchor
import com.vinyl.launcher.data.FontChoice
import androidx.compose.ui.Alignment

// Palet dasar: hitam piringan, krem label kertas, oranye hangat
val VinylBlack = Color(0xFF0E0E10)
val VinylSurface = Color(0xFF18181B)
val VinylSurfaceHigh = Color(0xFF232327)
val PaperCream = Color(0xFFF2EFEA)
val WarmAccent = Color(0xFFE07A3F)
val MutedText = Color(0xFF9B9893)

private val DarkScheme = darkColorScheme(
    primary = WarmAccent,
    onPrimary = Color(0xFF1A0E06),
    secondary = Color(0xFFC9B79C),
    background = VinylBlack,
    onBackground = PaperCream,
    surface = VinylSurface,
    onSurface = PaperCream,
    surfaceVariant = VinylSurfaceHigh,
    onSurfaceVariant = MutedText,
    surfaceContainer = VinylSurface,
    surfaceContainerHigh = VinylSurfaceHigh,
    surfaceContainerHighest = Color(0xFF2C2C31),
    outline = Color(0xFF3A3A40),
)

/** Tema aplikasi. Mode gelap adalah default (dan satu-satunya mode). */
@Composable
fun VinylTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkScheme, typography = Typography(), content = content)
}

// ===== Helper konversi pengaturan → nilai Compose =====

fun Long.toColor(): Color = Color(this)
fun Color.toLongArgb(): Long = toArgb().toLong() and 0xFFFFFFFFL
fun Int.toComposeColor(): Color = Color(this)

fun FontChoice.toFamily(): FontFamily = when (this) {
    FontChoice.DEFAULT -> FontFamily.Default
    FontChoice.SANS -> FontFamily.SansSerif
    FontChoice.SERIF -> FontFamily.Serif
    FontChoice.MONO -> FontFamily.Monospace
    FontChoice.CURSIVE -> FontFamily.Cursive
}

fun FontChoice.label(): String = when (this) {
    FontChoice.DEFAULT -> "Bawaan"
    FontChoice.SANS -> "Sans"
    FontChoice.SERIF -> "Serif"
    FontChoice.MONO -> "Mono"
    FontChoice.CURSIVE -> "Kursif"
}

fun Anchor.toAlignment(): Alignment = when (this) {
    Anchor.TOP_START -> Alignment.TopStart
    Anchor.TOP_CENTER -> Alignment.TopCenter
    Anchor.TOP_END -> Alignment.TopEnd
    Anchor.CENTER_START -> Alignment.CenterStart
    Anchor.CENTER -> Alignment.Center
    Anchor.CENTER_END -> Alignment.CenterEnd
    Anchor.BOTTOM_START -> Alignment.BottomStart
    Anchor.BOTTOM_CENTER -> Alignment.BottomCenter
    Anchor.BOTTOM_END -> Alignment.BottomEnd
}

fun Anchor.horizontal(): Alignment.Horizontal = when (this) {
    Anchor.TOP_START, Anchor.CENTER_START, Anchor.BOTTOM_START -> Alignment.Start
    Anchor.TOP_CENTER, Anchor.CENTER, Anchor.BOTTOM_CENTER -> Alignment.CenterHorizontally
    else -> Alignment.End
}

/** Warna teks yang terbaca di atas [bg]. */
fun contentColorFor(bg: Color): Color = if (bg.luminance() > 0.55f) Color(0xFF141414) else PaperCream

/** Gelapkan warna (untuk latar dari cover agar teks tetap terbaca). */
fun Color.darken(factor: Float): Color =
    Color(red * (1 - factor), green * (1 - factor), blue * (1 - factor), alpha)

fun Color.lighten(factor: Float): Color =
    Color(red + (1 - red) * factor, green + (1 - green) * factor, blue + (1 - blue) * factor, alpha)
