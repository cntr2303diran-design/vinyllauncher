package com.vinyl.launcher.ui.crate

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.provider.MediaStore
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vinyl.launcher.data.PlayRecord
import com.vinyl.launcher.media.MediaRepository
import com.vinyl.launcher.media.NowPlaying
import com.vinyl.launcher.ui.components.AmbientBackdrop
import com.vinyl.launcher.ui.components.glass
import com.vinyl.launcher.ui.theme.Accent
import com.vinyl.launcher.ui.theme.TextPrimary
import com.vinyl.launcher.ui.theme.TextSecondary
import com.vinyl.launcher.ui.theme.TextTertiary
import com.vinyl.launcher.util.rememberImage
import java.util.concurrent.TimeUnit

/**
 * "Rak piringan": lapisan layar penuh dengan dua tab
 * - Berikutnya: antrean lagu dari aplikasi musik, tampil sebagai deretan sampul
 * - Koleksi: riwayat lagu yang pernah diputar + statistik sederhana
 */
@Composable
fun CrateSheet(
    np: NowPlaying,
    queue: List<MediaRepository.QueueEntry>,
    history: List<PlayRecord>,
    fallback: Color,
    onClose: () -> Unit,
    onClearHistory: () -> Unit,
) {
    var tab by remember { mutableIntStateOf(if (queue.isNotEmpty()) 0 else 1) }
    Box(Modifier.fillMaxSize()) {
        AmbientBackdrop(np.art.takeIf { np.hasTrack }, np.swatches, fallback, animate = false, dim = 0.6f)
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            Row(
                Modifier.fillMaxWidth().padding(start = 36.dp, end = 20.dp, top = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Rak piringan", style = MaterialTheme.typography.displaySmall, color = TextPrimary, modifier = Modifier.weight(1f))
                Box(
                    Modifier.size(44.dp).clip(CircleShape).glass(CircleShape).clickable(onClick = onClose),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Close, "Tutup", tint = Color.White) }
            }
            Row(Modifier.padding(start = 32.dp, top = 18.dp, bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Berikutnya", "Koleksi").forEachIndexed { i, t ->
                    val sel = i == tab
                    Text(
                        t,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (sel) Color(0xFF111113) else Color.White,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (sel) Color.White.copy(alpha = 0.94f) else Color.White.copy(alpha = 0.08f))
                            .clickable { tab = i }
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                    )
                }
            }
            if (tab == 0) UpNext(queue) else Collection(history, onClearHistory)
        }
    }
}

// ======================= Berikutnya =======================

@Composable
private fun UpNext(queue: List<MediaRepository.QueueEntry>) {
    if (queue.isEmpty()) {
        EmptyState(
            "Antrean tidak tersedia",
            "Aplikasi musik yang sedang dipakai tidak membagikan daftar lagu berikutnya. " +
                "YouTube Music dan beberapa pemutar lokal biasanya membagikannya.",
        )
        return
    }
    val current = queue.indexOfFirst { it.isCurrent }.coerceAtLeast(0)
    val state = rememberLazyListState()
    LaunchedEffect(current, queue.size) { state.animateScrollToItem(current) }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
        LazyRow(
            state = state,
            contentPadding = PaddingValues(horizontal = 36.dp),
            horizontalArrangement = Arrangement.spacedBy(26.dp),
        ) {
            itemsIndexed(queue, key = { _, e -> e.id }) { i, e ->
                val img = rememberImage(e.art, e.artUri)
                Column(
                    Modifier.width(230.dp).clip(RoundedCornerShape(14.dp)).clickable { MediaRepository.playQueueItem(e.id) },
                ) {
                    Sleeve(img, 230.dp, highlight = e.isCurrent)
                    Spacer(Modifier.height(14.dp))
                    if (e.isCurrent) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.GraphicEq, null, tint = Accent, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("SEDANG DIPUTAR", color = Accent, style = MaterialTheme.typography.labelSmall)
                        }
                    } else {
                        Text(if (i > current) "BERIKUTNYA" else "SEBELUMNYA", color = TextTertiary, style = MaterialTheme.typography.labelSmall)
                    }
                    Text(e.title, style = MaterialTheme.typography.titleMedium, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
                    Text(e.subtitle, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

// ======================= Koleksi =======================

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Collection(history: List<PlayRecord>, onClear: () -> Unit) {
    val context = LocalContext.current
    var confirmClear by remember { mutableStateOf(false) }
    if (history.isEmpty()) {
        EmptyState("Koleksi masih kosong", "Lagu yang Anda dengarkan lebih dari 30 detik akan otomatis masuk ke rak ini.")
        return
    }
    val weekAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)
    val playsThisWeek = history.sumOf { r -> r.playTimes.count { it >= weekAgo } }
    val hours = history.sumOf { it.listenedMs } / 3_600_000.0
    val topArtists = history.groupBy { it.artist.ifBlank { "Tidak diketahui" } }
        .mapValues { (_, v) -> v.sumOf { it.plays } }
        .entries.sortedByDescending { it.value }.take(6)

    LazyVerticalGrid(
        columns = GridCells.Adaptive(150.dp),
        contentPadding = PaddingValues(horizontal = 32.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        // ---- Statistik ----
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                StatCard("$playsThisWeek", "diputar minggu ini", Modifier.weight(1f))
                StatCard("%.1f".format(hours), "jam mendengarkan", Modifier.weight(1f))
                StatCard("${history.size}", "lagu di koleksi", Modifier.weight(1f))
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column {
                Text("ARTIS TERATAS", style = MaterialTheme.typography.labelMedium, color = TextSecondary, modifier = Modifier.padding(start = 4.dp, bottom = 10.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    topArtists.forEachIndexed { i, (artist, plays) ->
                        Text(
                            "${i + 1}  $artist  ·  $plays×",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (i == 0) Color(0xFF111113) else Color.White,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (i == 0) Color.White.copy(alpha = 0.94f) else Color.White.copy(alpha = 0.08f))
                                .padding(horizontal = 16.dp, vertical = 9.dp),
                        )
                    }
                }
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("TERAKHIR DIPUTAR", style = MaterialTheme.typography.labelMedium, color = TextSecondary, modifier = Modifier.weight(1f).padding(start = 4.dp))
                TextButton(onClick = { confirmClear = true }) { Text("Hapus riwayat", color = TextSecondary) }
            }
        }
        // ---- Sampul-sampul ----
        items(history, key = { it.key }) { r ->
            val img = rememberImage(null, r.artFile)
            Column(Modifier.clip(RoundedCornerShape(12.dp)).clickable { playFromSearch(context, r) }) {
                Box {
                    Sleeve(img, null, highlight = false)
                    if (r.plays > 1) Text(
                        "${r.plays}×",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                            .glass(RoundedCornerShape(50), fillAlpha = 0.25f).padding(horizontal = 9.dp, vertical = 4.dp),
                    )
                }
                Text(r.title, style = MaterialTheme.typography.titleSmall, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 10.dp))
                Text(r.artist, style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
    if (confirmClear) AlertDialog(
        onDismissRequest = { confirmClear = false },
        title = { Text("Hapus semua riwayat?") },
        text = { Text("Koleksi dan statistik akan dikosongkan.") },
        confirmButton = { TextButton(onClick = { onClear(); confirmClear = false }) { Text("Hapus") } },
        dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Batal") } },
    )
}

@Composable
private fun StatCard(value: String, label: String, modifier: Modifier) {
    Column(modifier.glass(RoundedCornerShape(24.dp)).padding(horizontal = 20.dp, vertical = 18.dp)) {
        Text(value, style = MaterialTheme.typography.displaySmall, color = TextPrimary)
        Text(label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
    }
}

/** Sampul persegi ala lengan piringan, dengan bayangan lembut. */
@Composable
private fun Sleeve(img: ImageBitmap?, size: Dp?, highlight: Boolean) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        (if (size != null) Modifier.size(size) else Modifier.fillMaxWidth().aspectRatio(1f))
            .shadow(if (highlight) 30.dp else 14.dp, shape)
            .clip(shape)
            .background(Color(0xFF232327))
            .then(if (highlight) Modifier.border(2.dp, Accent, shape) else Modifier.border(0.6.dp, Color.White.copy(alpha = 0.1f), shape)),
        contentAlignment = Alignment.Center,
    ) {
        if (img != null) Image(img, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        else Icon(Icons.Rounded.MusicNote, null, tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(40.dp))
    }
}

@Composable
private fun EmptyState(title: String, body: String) {
    Box(Modifier.fillMaxSize().padding(40.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(420.dp)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
            Text(body, style = MaterialTheme.typography.bodyLarge, color = TextSecondary, modifier = Modifier.padding(top = 8.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

/** Putar ulang lagu dari koleksi lewat intent standar "putar dari pencarian" (didukung Spotify, YouTube Music, dll.). */
private fun playFromSearch(context: Context, r: PlayRecord) {
    fun intent(pkg: String?) = Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH).apply {
        putExtra(MediaStore.EXTRA_MEDIA_FOCUS, "vnd.android.cursor.item/audio")
        putExtra(MediaStore.EXTRA_MEDIA_TITLE, r.title)
        putExtra(MediaStore.EXTRA_MEDIA_ARTIST, r.artist)
        putExtra(SearchManager.QUERY, "${r.title} ${r.artist}")
        pkg?.let { setPackage(it) }
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(intent(r.pkg)) }
        .recoverCatching { context.startActivity(intent(null)) }
}

