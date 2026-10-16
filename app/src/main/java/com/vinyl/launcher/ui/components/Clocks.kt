package com.vinyl.launcher.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vinyl.launcher.data.ClockSettings
import com.vinyl.launcher.data.ClockType
import com.vinyl.launcher.ui.theme.toColor
import com.vinyl.launcher.ui.theme.toFamily
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Waktu sekarang, diperbarui tepat di pergantian detik. */
@Composable
fun rememberNow(): State<LocalDateTime> = produceState(LocalDateTime.now()) {
    while (true) {
        value = LocalDateTime.now()
        delay(1000L - System.currentTimeMillis() % 1000L)
    }
}

/** Teks jam sesuai format 12/24 jam. Mengembalikan (jam:menit, AM/PM atau null). */
fun timeParts(now: LocalDateTime, use24h: Boolean): Pair<String, String?> =
    if (use24h) now.format(DateTimeFormatter.ofPattern("HH:mm")) to null
    else now.format(DateTimeFormatter.ofPattern("h:mm")) to now.format(DateTimeFormatter.ofPattern("a", Locale.getDefault()))

fun formatDate(now: LocalDateTime): String =
    now.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault()))
        .replaceFirstChar { it.titlecase(Locale.getDefault()) }

/**
 * Jam dengan berbagai gaya. [baseSize] adalah ukuran font pada skala 1.0 (dp).
 * Gaya CIRCULAR di sini adalah versi mandiri (piringan kecil);
 * di layar kunci, jam melingkar digambar langsung di tepi vinyl besar.
 */
@Composable
fun ClockView(
    settings: ClockSettings,
    now: LocalDateTime,
    modifier: Modifier = Modifier,
    baseSize: Dp = 96.dp,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
) {
    val color = settings.color.toColor()
    val family = settings.font.toFamily()
    val fontSize = (baseSize.value * settings.scale).sp
    val (hm, ampm) = timeParts(now, settings.use24h)

    Column(modifier, horizontalAlignment = horizontalAlignment) {
        when (settings.type) {
            ClockType.THIN, ClockType.BOLD -> Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    hm,
                    color = color,
                    fontFamily = family,
                    fontSize = fontSize,
                    fontWeight = if (settings.type == ClockType.THIN) FontWeight.ExtraLight else FontWeight.Black,
                    letterSpacing = if (settings.type == ClockType.THIN) (-1).sp else 0.sp,
                    lineHeight = fontSize,
                )
                if (ampm != null) Text(
                    ampm, color = color.copy(alpha = 0.7f), fontFamily = family,
                    fontSize = fontSize * 0.28f, modifier = Modifier.padding(start = 6.dp, bottom = (baseSize * settings.scale) * 0.14f),
                )
            }
            ClockType.FLIP -> FlipClock(hm, ampm, color, family, baseSize * settings.scale)
            ClockType.ANALOG -> AnalogClock(now, color, baseSize * settings.scale * 1.7f)
            ClockType.CIRCULAR -> CircularClock(hm + (ampm?.let { " $it" } ?: ""), color, baseSize * settings.scale * 1.9f)
        }
        if (settings.showDate) {
            Text(
                formatDate(now),
                color = color.copy(alpha = 0.75f),
                fontFamily = family,
                fontSize = (baseSize.value * settings.scale * 0.2f).coerceAtLeast(12f).sp,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

/** Flip clock: setiap digit dalam kartu; digit baru "jatuh" dari atas. */
@Composable
private fun FlipClock(hm: String, ampm: String?, color: Color, family: androidx.compose.ui.text.font.FontFamily, size: Dp) {
    val fontSize = (size.value * 0.8f).sp
    Row(horizontalArrangement = Arrangement.spacedBy(size * 0.06f), verticalAlignment = Alignment.CenterVertically) {
        hm.forEach { ch ->
            if (ch == ':') {
                Text(":", color = color.copy(alpha = 0.6f), fontSize = fontSize * 0.8f, fontFamily = family)
            } else {
                Box(
                    Modifier
                        .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(size * 0.12f))
                        .clipToBounds()
                        .drawWithContent {
                            drawContent()
                            // Garis lipatan kartu flip
                            drawLine(
                                Color.Black.copy(alpha = 0.5f),
                                Offset(0f, this.size.height / 2f),
                                Offset(this.size.width, this.size.height / 2f),
                                strokeWidth = 2f,
                            )
                        }
                        .padding(horizontal = size * 0.1f, vertical = size * 0.02f),
                    contentAlignment = Alignment.Center,
                ) {
                    AnimatedContent(
                        targetState = ch,
                        transitionSpec = {
                            (slideInVertically(tween(380)) { -it } + fadeIn(tween(380))) togetherWith
                                (slideOutVertically(tween(380)) { it } + fadeOut(tween(250)))
                        },
                        label = "flip",
                    ) { c ->
                        Text(c.toString(), color = color, fontSize = fontSize, fontFamily = family, fontWeight = FontWeight.Medium, lineHeight = fontSize)
                    }
                }
            }
        }
        if (ampm != null) Text(ampm, color = color.copy(alpha = 0.7f), fontSize = fontSize * 0.3f, fontFamily = family)
    }
}

/** Jam analog minimalis dengan tick & jarum, digambar di Canvas. */
@Composable
fun AnalogClock(now: LocalDateTime, color: Color, size: Dp) {
    Canvas(Modifier.size(size)) {
        val r = this.size.minDimension / 2f
        val c = center
        drawCircle(color.copy(alpha = 0.08f), r, c)
        drawCircle(color.copy(alpha = 0.35f), r, c, style = Stroke(r * 0.015f))
        for (i in 0 until 60) {
            val long = i % 5 == 0
            rotate(i * 6f, c) {
                drawLine(
                    color.copy(alpha = if (long) 0.8f else 0.25f),
                    Offset(c.x, c.y - r * 0.92f),
                    Offset(c.x, c.y - r * (if (long) 0.78f else 0.86f)),
                    strokeWidth = r * (if (long) 0.025f else 0.01f),
                    cap = StrokeCap.Round,
                )
            }
        }
        val sec = now.second
        val min = now.minute + sec / 60f
        val hr = (now.hour % 12) + min / 60f
        rotate(hr * 30f, c) { drawLine(color, c, Offset(c.x, c.y - r * 0.5f), r * 0.05f, StrokeCap.Round) }
        rotate(min * 6f, c) { drawLine(color, c, Offset(c.x, c.y - r * 0.75f), r * 0.03f, StrokeCap.Round) }
        rotate(sec * 6f, c) { drawLine(Color(0xFFE07A3F), Offset(c.x, c.y + r * 0.12f), Offset(c.x, c.y - r * 0.82f), r * 0.012f, StrokeCap.Round) }
        drawCircle(Color(0xFFE07A3F), r * 0.04f, c)
    }
}

/** Jam melingkar mandiri: piringan kecil dengan waktu tertulis di tepi. */
@Composable
fun CircularClock(text: String, color: Color, size: Dp) {
    val paint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
            letterSpacing = 0.3f
        }
    }
    Canvas(Modifier.size(size)) {
        val r = this.size.minDimension / 2f
        drawCircle(Color(0xFF0B0B0D), r * 0.62f, center)
        for (i in 1..8) drawCircle(Color.White.copy(alpha = 0.05f), r * (0.3f + i * 0.04f), center, style = Stroke(1f))
        drawCircle(color.copy(alpha = 0.9f), r * 0.2f, center)
        drawCircularText("$text  •  $text  •", center, r * 0.82f, r * 0.17f, color, paint, startDeg = -90f, centered = true)
    }
}

/** Tanggal sebagai teks terpisah (dipakai layar kunci). */
@Composable
fun DateText(now: LocalDateTime, settings: ClockSettings, modifier: Modifier = Modifier, baseSize: Dp = 96.dp, textAlign: TextAlign = TextAlign.Start) {
    Text(
        formatDate(now),
        color = settings.color.toColor().copy(alpha = 0.8f),
        fontFamily = settings.font.toFamily(),
        fontSize = (baseSize.value * 0.22f).sp,
        textAlign = textAlign,
        modifier = modifier,
    )
}
