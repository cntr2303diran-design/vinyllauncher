@file:OptIn(ExperimentalTextApi::class)

package com.vinyl.launcher.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.vinyl.launcher.R
import com.vinyl.launcher.data.Anchor
import com.vinyl.launcher.data.FontChoice

/*
 * Bahasa desain "Vinyl":
 * - Gelap & tenang seperti Apple Music / layar kunci iOS: latar ambient dari warna cover,
 *   teks putih dengan 3 tingkat opasitas (primer, sekunder, tersier).
 * - Permukaan "kaca": putih transparan tipis + garis tepi halus (lihat GlassSurface).
 * - Pengelompokan ala One UI: kartu besar membulat, judul besar, banyak ruang kosong.
 * - Tipografi Inter (variable font) dengan tracking rapat untuk angka besar.
 */

// ===== Warna =====
val Ink = Color(0xFF09090B)
val InkElevated = Color(0xFF141416)
val InkCard = Color(0xFF1C1C1F)
val InkCardHigh = Color(0xFF26262A)
val Hairline = Color(0x1FFFFFFF)
val TextPrimary = Color(0xF5FFFFFF)
val TextSecondary = Color(0x99FFFFFF)
val TextTertiary = Color(0x61FFFFFF)
val Accent = Color(0xFFFF9F43)          // oranye hangat ala label piringan
val AccentOn = Color(0xFF1E1206)

// Nama lama dipertahankan agar komponen lain tetap kompatibel
val VinylBlack = Ink
val PaperCream = TextPrimary
val WarmAccent = Accent
val MutedText = TextSecondary

// ===== Font =====
private fun inter(weight: FontWeight, opsz: Float) = Font(
    R.font.inter, weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight), FontVariation.Setting("opsz", opsz)),
)

/** Inter untuk teks UI (optical size kecil). */
val InterText = FontFamily(
    listOf(FontWeight.Light, FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold).map { inter(it, 14f) }
)

/** Inter versi "display" untuk angka & judul besar (optical size besar = lebih rapat & elegan). */
val InterDisplay = FontFamily(
    listOf(
        FontWeight.ExtraLight, FontWeight.Light, FontWeight.Normal, FontWeight.Medium,
        FontWeight.SemiBold, FontWeight.Bold, FontWeight.ExtraBold, FontWeight.Black,
    ).map { inter(it, 32f) }
)

val SerifDisplay = FontFamily(Font(R.font.instrument_serif))

private fun mono(weight: FontWeight) = Font(
    R.font.jetbrains_mono, weight, variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)
val MonoFamily = FontFamily(listOf(FontWeight.Light, FontWeight.Normal, FontWeight.Medium, FontWeight.Bold).map { mono(it) })

private val DarkScheme = darkColorScheme(
    primary = Accent,
    onPrimary = AccentOn,
    primaryContainer = Color(0xFF3A2614),
    onPrimaryContainer = Color(0xFFFFDCC2),
    secondary = Color(0xFFD9C9B4),
    background = Ink,
    onBackground = TextPrimary,
    surface = InkElevated,
    onSurface = TextPrimary,
    surfaceVariant = InkCard,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = Ink,
    surfaceContainerLow = InkElevated,
    surfaceContainer = InkCard,
    surfaceContainerHigh = InkCardHigh,
    surfaceContainerHighest = Color(0xFF303035),
    outline = Color(0x33FFFFFF),
    outlineVariant = Hairline,
    errorContainer = Color(0xFF3B1716),
    onErrorContainer = Color(0xFFFFB4AB),
)

private val base = TextStyle(fontFamily = InterText, letterSpacing = 0.sp)

private val VinylTypography = Typography(
    displayLarge = base.copy(fontFamily = InterDisplay, fontWeight = FontWeight.Bold, fontSize = 57.sp, letterSpacing = (-0.04).em),
    displayMedium = base.copy(fontFamily = InterDisplay, fontWeight = FontWeight.Bold, fontSize = 45.sp, letterSpacing = (-0.035).em),
    displaySmall = base.copy(fontFamily = InterDisplay, fontWeight = FontWeight.Bold, fontSize = 36.sp, letterSpacing = (-0.03).em),
    headlineLarge = base.copy(fontFamily = InterDisplay, fontWeight = FontWeight.Bold, fontSize = 32.sp, letterSpacing = (-0.025).em),
    headlineMedium = base.copy(fontFamily = InterDisplay, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, letterSpacing = (-0.02).em),
    headlineSmall = base.copy(fontFamily = InterDisplay, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, letterSpacing = (-0.015).em),
    titleLarge = base.copy(fontWeight = FontWeight.SemiBold, fontSize = 20.sp, letterSpacing = (-0.01).em),
    titleMedium = base.copy(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    titleSmall = base.copy(fontWeight = FontWeight.Medium, fontSize = 14.sp),
    bodyLarge = base.copy(fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = base.copy(fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = base.copy(fontWeight = FontWeight.Normal, fontSize = 12.5.sp, lineHeight = 17.sp),
    labelLarge = base.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    labelMedium = base.copy(fontWeight = FontWeight.SemiBold, fontSize = 12.sp, letterSpacing = 0.02.em),
    labelSmall = base.copy(fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 0.03.em),
)

/** Tema aplikasi. Mode gelap adalah default (dan satu-satunya mode). */
@Composable
fun VinylTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkScheme, typography = VinylTypography, content = content)
}

// ===== Helper konversi pengaturan → nilai Compose =====

fun Long.toColor(): Color = Color(this)
fun Color.toLongArgb(): Long = toArgb().toLong() and 0xFFFFFFFFL
fun Int.toComposeColor(): Color = Color(this)

fun FontChoice.toFamily(): FontFamily = when (this) {
    FontChoice.DEFAULT -> InterDisplay
    FontChoice.SANS -> FontFamily.SansSerif
    FontChoice.SERIF -> SerifDisplay
    FontChoice.MONO -> MonoFamily
    FontChoice.CURSIVE -> FontFamily.Cursive
}

fun FontChoice.label(): String = when (this) {
    FontChoice.DEFAULT -> "Inter"
    FontChoice.SANS -> "Sistem"
    FontChoice.SERIF -> "Serif"
    FontChoice.MONO -> "Mono"
    FontChoice.CURSIVE -> "Tulisan tangan"
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
fun contentColorFor(bg: Color): Color = if (bg.luminance() > 0.55f) Color(0xFF141414) else TextPrimary

fun Color.darken(factor: Float): Color =
    Color(red * (1 - factor), green * (1 - factor), blue * (1 - factor), alpha)

fun Color.lighten(factor: Float): Color =
    Color(red + (1 - red) * factor, green + (1 - green) * factor, blue + (1 - blue) * factor, alpha)

/** Warna dengan saturasi & kecerahan dibatasi agar enak sebagai latar gelap. */
fun Color.ambient(maxValue: Float = 0.55f, minSat: Float = 0.25f): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(toArgb(), hsv)
    hsv[1] = hsv[1].coerceAtLeast(minSat).coerceAtMost(0.85f)
    hsv[2] = hsv[2].coerceIn(0.18f, maxValue)
    return Color(android.graphics.Color.HSVToColor(hsv))
}
