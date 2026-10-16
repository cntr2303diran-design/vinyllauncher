package com.vinyl.launcher.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.vinyl.launcher.data.VinylStyle
import com.vinyl.launcher.ui.theme.darken
import com.vinyl.launcher.ui.theme.lighten
import kotlin.math.min
import kotlin.math.roundToInt

/** Isi label tengah vinyl. */
sealed interface VinylLabel {
    /** Label warna dengan judul & artis melingkar (home screen). */
    data class Text(val title: String, val subtitle: String) : VinylLabel
    /** Label bergambar cover album (layar kunci). */
    data class Art(val art: ImageBitmap?) : VinylLabel
}

/**
 * Piringan vinyl yang digambar sepenuhnya dengan Canvas agar tajam di semua ukuran.
 * - Alur (groove) berupa lingkaran konsentris dengan variasi kecerahan, plus jeda antar-lagu.
 * - Kilap cahaya dibuat dengan sweep gradient yang TIDAK ikut berputar (sumber cahaya tetap),
 *   sehingga piringan tampak realistis saat berputar.
 * - Label, teks melingkar dan picture-disc ikut berputar sesuai [rotation].
 *
 * @param rotation sudut putar dalam derajat.
 * @param rimText teks opsional di tepi luar piringan (gaya jam "melingkar").
 */
@Composable
fun VinylDisc(
    modifier: Modifier = Modifier,
    rotation: Float,
    style: VinylStyle,
    labelColor: Color,
    discTint: Color,
    label: VinylLabel,
    art: ImageBitmap? = null,
    rimText: String? = null,
    rimTextColor: Color = Color.White,
) {
    // Paint native untuk teks pada path (Compose belum punya drawTextOnPath)
    val textPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            letterSpacing = 0.08f
        }
    }
    val rimPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
            letterSpacing = 0.25f
        }
    }

    Canvas(modifier) {
        val r = min(size.width, size.height) / 2f
        val c = center

        // ----- 1. Badan piringan -----
        val base = when (style) {
            VinylStyle.CLASSIC -> Color(0xFF0B0B0D)
            VinylStyle.COLORED -> discTint.darken(0.25f)
            VinylStyle.TRANSPARENT -> discTint.lighten(0.2f).copy(alpha = 0.45f)
            VinylStyle.PICTURE_DISC -> Color(0xFF151517)
        }
        drawCircle(base, r, c)

        if (style == VinylStyle.PICTURE_DISC && art != null) {
            rotate(rotation, c) { drawImageInCircle(art, c, r * 0.985f) }
            // Lapisan gelap tipis agar alur tetap terlihat
            drawCircle(Color.Black.copy(alpha = 0.18f), r, c)
        }

        // ----- 2. Alur (tidak perlu dirotasi; konsentris) -----
        drawGrooves(c, r, style)

        // Bibir piringan
        drawCircle(Color.White.copy(alpha = 0.08f), r - 1f, c, style = Stroke(width = 2f))

        // ----- 3. Kilap cahaya (statis, tidak ikut berputar) -----
        val sheenAlpha = if (style == VinylStyle.TRANSPARENT) 0.10f else 0.16f
        drawCircle(
            brush = Brush.sweepGradient(
                0.00f to Color.Transparent,
                0.06f to Color.White.copy(alpha = sheenAlpha),
                0.13f to Color.Transparent,
                0.50f to Color.Transparent,
                0.56f to Color.White.copy(alpha = sheenAlpha * 0.8f),
                0.63f to Color.Transparent,
                1.00f to Color.Transparent,
                center = c,
            ),
            radius = r * 0.97f,
            center = c,
        )

        // ----- 4. Label tengah (berputar) -----
        val labelR = r * 0.34f
        rotate(rotation, c) {
            when (label) {
                is VinylLabel.Art -> {
                    if (label.art != null) drawImageInCircle(label.art, c, labelR)
                    else drawCircle(labelColor, labelR, c)
                }
                is VinylLabel.Text -> {
                    if (style != VinylStyle.PICTURE_DISC || art == null) {
                        drawCircle(
                            Brush.radialGradient(
                                listOf(labelColor.lighten(0.08f), labelColor, labelColor.darken(0.12f)),
                                center = c, radius = labelR,
                            ),
                            labelR, c,
                        )
                    }
                    drawCircle(Color.Black.copy(alpha = 0.18f), labelR * 0.93f, c, style = Stroke(1.2f))
                    val ink = if (labelColor.luminanceApprox() > 0.55f) Color(0xFF151515) else Color(0xFFF7F3EC)
                    drawCircularText(label.title.uppercase(), c, labelR * 0.72f, labelR * 0.16f, ink, textPaint)
                    drawCircularText(label.subtitle, c, labelR * 0.50f, labelR * 0.12f, ink.copy(alpha = 0.75f), textPaint, startDeg = 110f)
                }
            }
        }
        // Lubang as (spindle)
        drawCircle(Color(0xFFD9D9DC), r * 0.022f, c)
        drawCircle(Color.Black.copy(alpha = 0.6f), r * 0.012f, c)

        // ----- 5. Teks di tepi (jam melingkar) -----
        if (!rimText.isNullOrBlank()) {
            drawCircularText(rimText, c, r * 0.9f, r * 0.07f, rimTextColor, rimPaint, startDeg = -90f, centered = true)
        }
    }
}

private fun Color.luminanceApprox(): Float = 0.2126f * red + 0.7152f * green + 0.0722f * blue

/** Gambar alur dengan beberapa "jeda antar-lagu" yang sedikit lebih terang. */
private fun DrawScope.drawGrooves(c: Offset, r: Float, style: VinylStyle) {
    val inner = r * 0.37f
    val outer = r * 0.965f
    val step = ((outer - inner) / 70f).coerceAtLeast(1.5f)
    val baseAlpha = when (style) {
        VinylStyle.CLASSIC -> 0.055f
        VinylStyle.TRANSPARENT -> 0.08f
        else -> 0.07f
    }
    val gaps = floatArrayOf(0.48f, 0.61f, 0.73f, 0.86f) // posisi jeda lagu (fraksi radius)
    var rr = inner
    var i = 0
    while (rr < outer) {
        val frac = rr / r
        val isGap = gaps.any { kotlin.math.abs(it - frac) < 0.006f }
        val a = when {
            isGap -> baseAlpha * 0.2f
            i % 3 == 0 -> baseAlpha * 1.6f
            else -> baseAlpha
        }
        val col = if (isGap) Color.Black.copy(alpha = 0.35f) else Color.White.copy(alpha = a)
        drawCircle(col, rr, c, style = Stroke(width = if (isGap) step * 1.4f else 0.8f))
        rr += step
        i++
    }
    // Area lead-in di tepi & run-out di dekat label: lebih halus
    drawCircle(Color.White.copy(alpha = 0.03f), (inner + r * 0.03f), c, style = Stroke(r * 0.05f))
}

/** Gambar bitmap yang dipotong lingkaran, mengisi (center-crop) radius [radius]. */
fun DrawScope.drawImageInCircle(img: ImageBitmap, c: Offset, radius: Float) {
    val path = Path().apply { addOval(androidx.compose.ui.geometry.Rect(c, radius)) }
    clipPath(path) {
        val side = min(img.width, img.height)
        val srcOffset = IntOffset((img.width - side) / 2, (img.height - side) / 2)
        drawImage(
            image = img,
            srcOffset = srcOffset,
            srcSize = IntSize(side, side),
            dstOffset = IntOffset((c.x - radius).roundToInt(), (c.y - radius).roundToInt()),
            dstSize = IntSize((radius * 2).roundToInt(), (radius * 2).roundToInt()),
        )
    }
}

/**
 * Menulis teks mengikuti lingkaran berjari-jari [radius].
 * @param startDeg sudut awal (0° = arah jam 3, searah jarum jam).
 * @param centered jika true, teks dipusatkan di sekitar [startDeg].
 */
fun DrawScope.drawCircularText(
    text: String,
    c: Offset,
    radius: Float,
    textSize: Float,
    color: Color,
    paint: Paint,
    startDeg: Float = 180f,
    centered: Boolean = false,
) {
    if (text.isBlank() || radius <= 0f) return
    paint.textSize = textSize
    paint.color = color.toArgb()
    val circumference = (2 * Math.PI * radius).toFloat()
    // Potong teks yang terlalu panjang agar tidak menumpuk
    var t = text
    val maxLen = circumference * 0.92f
    while (paint.measureText(t) > maxLen && t.length > 2) t = t.dropLast(2) + "…"
    val textLen = paint.measureText(t)
    val startAngle = if (centered) startDeg - (textLen / circumference) * 180f else startDeg
    val path = android.graphics.Path().apply {
        addArc(c.x - radius, c.y - radius, c.x + radius, c.y + radius, startAngle, 359.9f)
    }
    drawIntoCanvas { canvas ->
        canvas.nativeCanvas.drawTextOnPath(t, path, 0f, textSize * 0.35f, paint)
    }
}

/** Ukuran default untuk pratinjau. */
internal val PreviewDiscSize = Size(400f, 400f)
