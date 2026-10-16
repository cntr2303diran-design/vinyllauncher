package com.vinyl.launcher.ui.components

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.vinyl.launcher.data.TonearmStyle
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Geometri tonearm. Sudut θ dalam radian: 0 = lengan lurus ke bawah,
 * positif = berayun ke kiri (ke arah tengah piringan).
 */
data class TonearmGeometry(val pivot: Offset, val length: Float, val discCenter: Offset, val discRadius: Float) {

    fun direction(theta: Float) = Offset(-sin(theta), cos(theta))

    fun needle(theta: Float): Offset = pivot + direction(theta) * length

    /** Sudut di mana lengan mengarah tepat ke pusat piringan (batas atas pencarian). */
    private val maxTheta: Float = run {
        val v = discCenter - pivot
        atan2(-v.x, v.y)
    }

    /** Jarak jarum ke pusat piringan untuk sudut θ. */
    fun needleRadius(theta: Float): Float = (needle(theta) - discCenter).getDistance()

    /** Sudut agar jarum berada di radius [r] (pencarian biner, jarak menurun monoton). */
    fun thetaForRadius(r: Float): Float {
        var lo = 0f
        var hi = maxTheta
        repeat(22) {
            val mid = (lo + hi) / 2f
            if (needleRadius(mid) > r) lo = mid else hi = mid
        }
        return (lo + hi) / 2f
    }

    /** Alur terluar → progress 0, alur terdalam → progress 1. */
    fun radiusForProgress(p: Float) = discRadius * (OUTER - (OUTER - INNER) * p.coerceIn(0f, 1f))
    fun progressForRadius(r: Float) = ((OUTER * discRadius - r) / ((OUTER - INNER) * discRadius)).coerceIn(0f, 1f)

    fun thetaForProgress(p: Float) = thetaForRadius(radiusForProgress(p))

    /** Sudut lengan yang menunjuk ke titik sentuh [p]. */
    fun thetaToward(p: Offset): Float {
        val v = p - pivot
        return atan2(-v.x, v.y)
    }

    /** Jarak titik [p] ke batang lengan pada sudut θ (untuk hit-test sentuhan). */
    fun distanceToArm(p: Offset, theta: Float): Float {
        val a = pivot
        val b = needle(theta)
        val ab = b - a
        val t = (((p - a).x * ab.x + (p - a).y * ab.y) / (ab.x * ab.x + ab.y * ab.y)).coerceIn(0f, 1f)
        val proj = a + ab * t
        return hypot(p.x - proj.x, p.y - proj.y)
    }

    companion object {
        const val OUTER = 0.94f
        const val INNER = 0.42f
        const val REST_THETA = -0.07f
    }
}

private data class ArmPalette(val tube: List<Color>, val base: Color, val accent: Color, val head: Color)

private fun paletteFor(style: TonearmStyle) = when (style) {
    TonearmStyle.SILVER -> ArmPalette(
        listOf(Color(0xFF8E9096), Color(0xFFF1F2F4), Color(0xFFA9ABB1)),
        Color(0xFF1A1A1D), Color(0xFFCFD1D6), Color(0xFF121214),
    )
    TonearmStyle.BLACK -> ArmPalette(
        listOf(Color(0xFF111113), Color(0xFF4A4A50), Color(0xFF1C1C1F)),
        Color(0xFF0D0D0F), Color(0xFF6B6B72), Color(0xFF0A0A0B),
    )
    TonearmStyle.GOLD -> ArmPalette(
        listOf(Color(0xFF8C6A28), Color(0xFFF4D98B), Color(0xFFB08A3C)),
        Color(0xFF1A1610), Color(0xFFE6C36A), Color(0xFF16120B),
    )
}

/**
 * Menggambar tonearm perak-hitam: dudukan, pemberat, batang, headshell & jarum.
 * @param lift 0 = jarum di piringan, 1 = terangkat (bayangan lebih jauh & headshell sedikit membesar).
 */
fun DrawScope.drawTonearm(geo: TonearmGeometry, theta: Float, lift: Float, style: TonearmStyle) {
    val pal = paletteFor(style)
    val r = geo.discRadius
    val deg = Math.toDegrees(theta.toDouble()).toFloat()
    val tubeW = r * 0.035f
    val shadowOffset = Offset(r * (0.02f + 0.05f * lift), r * (0.03f + 0.07f * lift))

    // Dudukan (plinth) — tidak ikut berputar
    drawCircle(Color.Black.copy(alpha = 0.35f), r * 0.15f, geo.pivot + Offset(r * 0.02f, r * 0.03f))
    drawCircle(pal.base, r * 0.15f, geo.pivot)
    drawCircle(pal.accent.copy(alpha = 0.25f), r * 0.15f, geo.pivot, style = Stroke(r * 0.008f))
    drawCircle(pal.base, r * 0.10f, geo.pivot)
    drawCircle(
        Brush.radialGradient(pal.tube, center = geo.pivot - Offset(r * 0.02f, r * 0.02f), radius = r * 0.08f),
        r * 0.07f, geo.pivot,
    )

    // Lengan digambar vertikal (ke bawah) lalu dirotasi sebesar θ di sekitar pivot
    val len = geo.length
    fun armShapes(shadow: Boolean) {
        val o = if (shadow) shadowOffset else Offset.Zero
        val p = geo.pivot + o
        val tubeBrush = if (shadow) Brush.linearGradient(listOf(Color.Black.copy(alpha = 0.35f), Color.Black.copy(alpha = 0.35f)))
        else Brush.horizontalGradient(pal.tube, startX = p.x - tubeW, endX = p.x + tubeW)
        rotate(deg, p) {
            // Pemberat di belakang pivot
            drawRoundRect(
                brush = if (shadow) tubeBrush else Brush.horizontalGradient(
                    listOf(pal.base, pal.accent.copy(alpha = 0.5f), pal.base), startX = p.x - r * 0.07f, endX = p.x + r * 0.07f,
                ),
                topLeft = Offset(p.x - r * 0.065f, p.y - len * 0.26f),
                size = Size(r * 0.13f, len * 0.12f),
                cornerRadius = CornerRadius(r * 0.02f),
            )
            // Batang
            drawLine(tubeBrush, p - Offset(0f, len * 0.15f), p + Offset(0f, len * 0.86f), tubeW, StrokeCap.Round)
            // Headshell (sedikit membesar saat terangkat = ilusi lebih dekat ke mata)
            val hs = 1f + 0.06f * lift
            val hw = r * 0.085f * hs
            val hh = len * 0.14f * hs
            val headTop = p + Offset(-hw / 2f, len * 0.86f)
            drawRoundRect(
                color = if (shadow) Color.Black.copy(alpha = 0.35f) else pal.head,
                topLeft = headTop,
                size = Size(hw, hh),
                cornerRadius = CornerRadius(r * 0.012f),
            )
            if (!shadow) {
                // Garis aksen & pengangkat jari
                drawLine(pal.accent, headTop + Offset(hw * 0.2f, hh * 0.2f), headTop + Offset(hw * 0.2f, hh * 0.85f), r * 0.006f)
                drawLine(pal.accent, headTop + Offset(hw, hh * 0.25f), headTop + Offset(hw + r * 0.05f, hh * 0.1f), r * 0.012f, StrokeCap.Round)
                // Cartridge & jarum
                drawCircle(pal.accent, r * 0.009f, p + Offset(0f, len * 0.97f))
            }
        }
    }
    armShapes(shadow = true)
    armShapes(shadow = false)
}
