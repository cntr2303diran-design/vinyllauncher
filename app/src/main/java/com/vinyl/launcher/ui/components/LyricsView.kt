package com.vinyl.launcher.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.vinyl.launcher.data.Lyrics
import com.vinyl.launcher.data.LyricsRepository
import com.vinyl.launcher.media.NowPlaying
import com.vinyl.launcher.ui.theme.InterDisplay
import kotlinx.coroutines.delay

/** Muat lirik untuk lagu yang sedang diputar (null saat memuat / tidak ditemukan). */
@Composable
fun rememberLyrics(np: NowPlaying, enabled: Boolean): Lyrics? {
    val lyrics by produceState<Lyrics?>(null, np.title, np.artist, enabled) {
        value = null
        if (enabled && np.hasTrack) value = LyricsRepository.get(np.title, np.artist, np.album, np.durationMs)
    }
    return lyrics
}

/**
 * Dua baris lirik ala Apple Music: baris aktif tebal & terang, baris berikutnya redup.
 * Pergantian baris dianimasikan naik ke atas.
 */
@Composable
fun LyricsLines(
    np: NowPlaying,
    lyrics: Lyrics?,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 24.sp,
    align: TextAlign = TextAlign.Center,
) {
    if (lyrics == null || !lyrics.hasSynced) return
    var index by remember(lyrics) { mutableIntStateOf(lyrics.indexAt(np.positionAt())) }
    LaunchedEffect(np, lyrics) {
        while (true) {
            // sedikit lebih awal (150 ms) agar terasa pas dengan suara
            index = lyrics.indexAt(np.positionAt() + 150)
            if (!np.isPlaying) break
            delay(120)
        }
    }
    val horizontal = when (align) {
        TextAlign.Start -> Alignment.Start
        TextAlign.End -> Alignment.End
        else -> Alignment.CenterHorizontally
    }
    AnimatedContent(
        targetState = index,
        transitionSpec = {
            (slideInVertically(tween(420)) { it / 2 } + fadeIn(tween(420))) togetherWith
                (slideOutVertically(tween(380)) { -it / 2 } + fadeOut(tween(260)))
        },
        modifier = modifier,
        label = "lyrics",
    ) { i ->
        Column(horizontalAlignment = horizontal) {
            val current = lyrics.synced.getOrNull(i)?.text.orEmpty().ifBlank { if (i >= 0) "♪" else "" }
            val next = lyrics.synced.getOrNull(i + 1)?.text.orEmpty()
            Text(
                current,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = InterDisplay, fontWeight = FontWeight.Bold, fontSize = fontSize, letterSpacing = (-0.015).em,
                ),
                color = Color.White.copy(alpha = 0.95f),
                textAlign = align,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (next.isNotBlank()) Text(
                next,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = fontSize * 0.68f, fontWeight = FontWeight.SemiBold),
                color = Color.White.copy(alpha = 0.38f),
                textAlign = align,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
