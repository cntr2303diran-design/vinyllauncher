package com.vinyl.launcher.ui.components

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vinyl.launcher.ui.theme.InterText
import com.vinyl.launcher.ui.theme.ambient
import kotlinx.coroutines.isActive
import kotlin.math.cos
import kotlin.math.sin

/**
 * Permukaan "kaca" (terinspirasi Liquid Glass iOS & panel One UI):
 * isi putih transparan tipis, garis tepi bergradasi (lebih terang di atas),
 * dan kilau lembut di bagian atas.
 */
fun Modifier.glass(
    shape: Shape = RoundedCornerShape(24.dp),
    tint: Color = Color.White,
    fillAlpha: Float = 0.09f,
): Modifier = this
    .clip(shape)
    .background(tint.copy(alpha = fillAlpha), shape)
    .background(
        Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.07f), Color.Transparent, Color.Transparent)),
        shape,
    )
    .border(
        width = 0.8.dp,
        brush = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.28f), Color.White.copy(alpha = 0.06f))),
        shape = shape,
    )

/** Panel gelap pekat untuk editor/sheet yang menutupi konten. */
fun Modifier.sheet(shape: Shape): Modifier = this
    .clip(shape)
    .background(Color(0xF2121214), shape)
    .border(0.8.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.16f), Color.Transparent)), shape)

@Composable
fun GlassBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier.glass(shape), content = content)
}

/**
 * Latar ambient ala Apple Music: cover yang di-blur sangat kuat + gumpalan warna
 * dari palet cover yang melayang pelan. Bergerak hanya saat musik diputar (hemat baterai).
 *
 * @param dim seberapa gelap lapisan penutup (0..1), agar teks tetap terbaca.
 */
@Composable
fun AmbientBackdrop(
    art: ImageBitmap?,
    swatches: List<Int>,
    fallback: Color,
    animate: Boolean,
    modifier: Modifier = Modifier,
    dim: Float = 0.38f,
) {
    // Cover diperkecil jadi 24px: saat direntangkan, interpolasi bilinear menghasilkan blur alami
    // (berfungsi di semua versi Android; di Android 12+ ditambah blur RenderEffect).
    val tiny = remember(art) {
        art?.let {
            runCatching {
                android.graphics.Bitmap.createScaledBitmap(it.asAndroidBitmap(), 24, 24, true).asImageBitmap()
            }.getOrNull()
        }
    }
    val colors = remember(swatches, fallback) {
        val base = swatches.map { Color(it).ambient() }
        (base.ifEmpty { listOf(fallback.ambient(0.4f, 0.1f)) } + listOf(fallback.ambient(0.35f, 0.1f))).take(4)
    }
    val c0 by animateColorAsState(colors.getOrElse(0) { fallback }, tween(1200), label = "c0")
    val c1 by animateColorAsState(colors.getOrElse(1) { c0 }, tween(1200), label = "c1")
    val c2 by animateColorAsState(colors.getOrElse(2) { c1 }, tween(1200), label = "c2")
    val base by animateColorAsState(colors.last().copy(alpha = 1f).let { Color(it.red * 0.35f, it.green * 0.35f, it.blue * 0.35f) }, tween(1200), label = "base")

    // Waktu animasi (detik), hanya maju saat musik diputar
    var t by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(animate) {
        if (!animate) return@LaunchedEffect
        var last = 0L
        while (isActive) {
            withFrameNanos { now ->
                if (last != 0L) t += (now - last) / 1e9f
                last = now
            }
        }
    }

    Box(modifier.fillMaxSize().background(base)) {
        Crossfade(targetState = tiny, animationSpec = tween(900), label = "ambientArt") { img ->
            if (img != null) {
                Image(
                    img, null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = 1.35f; scaleY = 1.35f
                            rotationZ = (t * 2.2f) % 360f   // putaran sangat pelan
                            alpha = 0.85f
                        }
                        .then(if (Build.VERSION.SDK_INT >= 31) Modifier.blur(48.dp) else Modifier),
                )
            }
        }
        // Gumpalan warna melayang
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width; val h = size.height
            val r = maxOf(w, h) * 0.55f
            fun blob(color: Color, cx: Float, cy: Float, radius: Float, a: Float) = drawCircle(
                Brush.radialGradient(listOf(color.copy(alpha = a), color.copy(alpha = 0f)), center = Offset(cx, cy), radius = radius),
                radius, Offset(cx, cy),
            )
            blob(c0, w * (0.25f + 0.12f * sin(t * 0.11f)), h * (0.30f + 0.15f * cos(t * 0.09f)), r, 0.55f)
            blob(c1, w * (0.78f + 0.10f * cos(t * 0.07f)), h * (0.70f + 0.12f * sin(t * 0.08f)), r * 0.9f, 0.5f)
            blob(c2, w * (0.55f + 0.18f * sin(t * 0.05f + 2f)), h * (0.15f + 0.10f * cos(t * 0.06f + 1f)), r * 0.7f, 0.35f)
        }
        // Penggelap + vignette agar teks putih selalu terbaca
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color.Black.copy(alpha = dim * 0.7f), Color.Black.copy(alpha = dim), Color.Black.copy(alpha = dim * 1.35f)))
            )
        )
    }
}

/** Teks dengan kilau yang menyapu dari kiri ke kanan — penghormatan untuk "slide to unlock". */
@Composable
fun ShimmerText(text: String, modifier: Modifier = Modifier, fontSize: TextUnit = 17.sp, width: Dp = 360.dp) {
    val x by rememberInfiniteTransition(label = "shimmer").animateFloat(
        initialValue = -0.4f, targetValue = 1.4f,
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Restart),
        label = "x",
    )
    val px = width.value * 3f
    Text(
        text,
        modifier = modifier,
        style = TextStyle(
            fontFamily = InterText,
            fontWeight = FontWeight.Medium,
            fontSize = fontSize,
            brush = Brush.linearGradient(
                0f to Color.White.copy(alpha = 0.38f),
                (x - 0.12f).coerceIn(0f, 1f) to Color.White.copy(alpha = 0.38f),
                x.coerceIn(0f, 1f) to Color.White.copy(alpha = 0.95f),
                (x + 0.12f).coerceIn(0f, 1f) to Color.White.copy(alpha = 0.38f),
                1f to Color.White.copy(alpha = 0.38f),
                start = Offset(0f, 0f), end = Offset(px, 0f),
            ),
        ),
    )
}
