package com.vinyl.launcher.ui.components

import android.graphics.BitmapFactory
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import com.vinyl.launcher.data.BackgroundSettings
import com.vinyl.launcher.data.BackgroundType
import com.vinyl.launcher.ui.theme.toColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Muat gambar dari file internal dengan downsampling sesuai ukuran layar. */
@Composable
fun rememberFileImage(path: String?): ImageBitmap? {
    val config = LocalConfiguration.current
    val density = LocalDensity.current.density
    val maxPx = (maxOf(config.screenWidthDp, config.screenHeightDp) * density).toInt().coerceAtLeast(512)
    val img by produceState<ImageBitmap?>(null, path, maxPx) {
        value = if (path == null) null else withContext(Dispatchers.IO) {
            runCatching {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(path, bounds)
                var sample = 1
                while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxPx) sample *= 2
                BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })?.asImageBitmap()
            }.getOrNull()
        }
    }
    return img
}

/**
 * Latar home screen / layar kunci.
 * @param albumColor warna dominan cover (sudah dianimasikan oleh pemanggil atau di sini).
 * @param glow jika true (layar kunci), mode ALBUM menjadi latar gelap dengan cahaya lembut warna cover.
 */
@Composable
fun BackgroundLayer(
    settings: BackgroundSettings,
    albumColor: Color,
    modifier: Modifier = Modifier,
    glow: Boolean = false,
) {
    // Transisi halus saat lagu berganti
    val animatedAlbum by animateColorAsState(albumColor, tween(900), label = "bg")
    Box(modifier.fillMaxSize()) {
        when (settings.type) {
            BackgroundType.ALBUM -> {
                if (glow) {
                    Box(
                        Modifier.fillMaxSize()
                            .background(Color(0xFF09090A))
                            .background(
                                Brush.radialGradient(
                                    0f to animatedAlbum.copy(alpha = 0.55f),
                                    0.55f to animatedAlbum.copy(alpha = 0.18f),
                                    1f to Color.Transparent,
                                    radius = 1600f,
                                )
                            )
                    )
                } else {
                    Box(Modifier.fillMaxSize().background(animatedAlbum))
                }
            }
            BackgroundType.SOLID -> Box(Modifier.fillMaxSize().background(settings.solidColor.toColor()))
            BackgroundType.GRADIENT -> Box(
                Modifier.fillMaxSize().background(
                    Brush.linearGradient(listOf(settings.gradientStart.toColor(), settings.gradientEnd.toColor()))
                )
            )
            BackgroundType.IMAGE -> {
                val img = rememberFileImage(settings.imagePath)
                Box(Modifier.fillMaxSize().background(Color(0xFF0E0E10)))
                if (img != null) {
                    Image(img, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    // Redupkan sedikit agar jam & turntable tetap menonjol
                    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.25f)))
                }
            }
        }
    }
}
