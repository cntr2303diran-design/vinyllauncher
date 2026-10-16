package com.vinyl.launcher.ui.crate

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.provider.MediaStore
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
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
    var tab by remember { mutableIntStateOf(0) }
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
                listOf("Rak", "Koleksi").forEachIndexed { i, t ->
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
            if (tab == 0) RecordShelf(np, queue, history) else Collection(history, onClearHistory)
        }
    }
}

// ======================= Rak (Sebelumnya / Berikutnya) =======================

/** Satu sampul di rak. */
private data class ShelfItem(
    val key: String,
    val title: String,
    val subtitle: String,
    val bitmap: android.graphics.Bitmap?,
    val source: String?,
    val art: ImageBitmap?,
    val relation: Int, // <0 sebelumnya, 0 sedang diputar, >0 berikutnya
    val play: (() -> Unit)?,
)

/**
 * Rak piringan ala "cover flow": sampul-sampul berdiri di atas papan kayu.
 * Geser ke kanan/kiri untuk melihat lagu sebelumnya & berikutnya; ketuk sampul di tengah untuk memutarnya.
 * Sumber: antrean aplikasi musik bila tersedia, jika tidak → riwayat (sebelumnya) + lagu saat ini.
 */
@Composable
private fun RecordShelf(np: NowPlaying, queue: List<MediaRepository.QueueEntry>, history: List<PlayRecord>) {
    val context = LocalContext.current
    val items = remember(np.title, np.artist, np.art, queue, history) {
        if (queue.isNotEmpty()) {
            val cur = queue.indexOfFirst { it.isCurrent }.let { if (it < 0) 0 else it }
            queue.mapIndexed { i, e ->
                ShelfItem("q${e.id}", e.title, e.subtitle, e.art, e.artUri, null, i - cur, { MediaRepository.playQueueItem(e.id) })
            }
        } else {
            val currentKey = "${np.title.trim().lowercase()}|${np.artist.trim().lowercase()}"
            val past = history.filter { it.key != currentKey }.take(14).reversed().mapIndexed { i, r ->
                ShelfItem("h${r.key}", r.title, r.artist, null, r.artFile, null, -(14 - i), { playFromSearch(context, r) })
            }
            val now = if (np.hasTrack) listOf(ShelfItem("now", np.title, np.artist, null, null, np.art, 0, null)) else emptyList()
            past + now
        }
    }
    if (items.isEmpty()) {
        EmptyState("Rak masih kosong", "Putar beberapa lagu dulu — lagu sebelumnya dan berikutnya akan berjejer di rak ini.")
        return
    }
    val startPage = items.indexOfFirst { it.relation == 0 }.let { if (it < 0) items.lastIndex else it }
    val pager = rememberPagerState(initialPage = startPage) { items.size }
    val scope = rememberCoroutineScope()
    LaunchedEffect(startPage, items.size) { pager.animateScrollToPage(startPage) }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val sleeve = minOf(maxHeight * 0.52f, maxWidth * 0.30f, 340.dp)
        val step = sleeve * 0.62f
        val focused = items.getOrNull(pager.currentPage)

        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.weight(0.2f))
            Box(Modifier.fillMaxWidth().height(sleeve + 70.dp)) {
                // Papan kayu (digambar di belakang sampul)
                Shelf(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(70.dp))
                HorizontalPager(
                    state = pager,
                    pageSize = PageSize.Fixed(step),
                    contentPadding = PaddingValues(horizontal = (maxWidth - step) / 2),
                    beyondViewportPageCount = 3,
                    modifier = Modifier.fillMaxWidth().height(sleeve + 30.dp).align(Alignment.TopCenter),
                    verticalAlignment = Alignment.Bottom,
                ) { page ->
                    val item = items[page]
                    val img = item.art ?: rememberImage(item.bitmap, item.source)
                    val offset = (pager.currentPage - page) + pager.currentPageOffsetFraction
                    val abs = kotlin.math.abs(offset).coerceAtMost(2f)
                    Box(
                        Modifier
                            .requiredSize(sleeve)
                            .zIndex(-abs)
                            .graphicsLayer {
                                cameraDistance = 14f * density
                                rotationY = (offset.coerceIn(-1.4f, 1.4f)) * 42f
                                val sc = 1f - 0.16f * abs.coerceAtMost(1.5f)
                                scaleX = sc; scaleY = sc
                                transformOrigin = TransformOrigin(0.5f, 1f) // berdiri di atas papan
                                alpha = 1f - 0.25f * (abs - 1f).coerceAtLeast(0f)
                            }
                            .clickable {
                                if (page != pager.currentPage) scope.launch { pager.animateScrollToPage(page) }
                                else item.play?.invoke()
                            },
                    ) {
                        Sleeve(img, null, highlight = item.relation == 0)
                    }
                }
            }
            // Info sampul yang sedang di tengah
            Spacer(Modifier.height(22.dp))
            if (focused != null) {
                val (label, color) = when {
                    focused.relation == 0 -> "SEDANG DIPUTAR" to Accent
                    focused.relation < 0 -> "SEBELUMNYA" to TextTertiary
                    else -> "BERIKUTNYA" to TextTertiary
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (focused.relation == 0) {
                        Icon(Icons.Rounded.GraphicEq, null, tint = Accent, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(label, color = color, style = MaterialTheme.typography.labelMedium)
                }
                Text(focused.title, style = MaterialTheme.typography.headlineSmall, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp, start = 40.dp, end = 40.dp))
                Text(focused.subtitle, style = MaterialTheme.typography.bodyLarge, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (focused.relation != 0 && focused.play != null) Text(
                    "Ketuk sampul untuk memutar",
                    style = MaterialTheme.typography.bodySmall, color = TextTertiary, modifier = Modifier.padding(top = 8.dp),
                )
            }
            Spacer(Modifier.weight(0.3f))
        }
    }
}

/** Papan rak kayu walnut: permukaan atas terang, sisi depan lebih gelap, serat kayu halus, bayangan di bawah. */
@Composable
private fun Shelf(modifier: Modifier) {
    Canvas(modifier) {
        val w = size.width
        val topH = size.height * 0.32f
        val frontH = size.height * 0.38f
        val y0 = 0f
        // Bayangan lembut di bawah papan
        drawRect(
            Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.45f), Color.Transparent), startY = y0 + topH + frontH, endY = size.height),
            topLeft = Offset(0f, y0 + topH + frontH), size = Size(w, size.height - topH - frontH),
        )
        // Permukaan atas (perspektif: sedikit lebih terang)
        drawRect(
            Brush.verticalGradient(listOf(Color(0xFF6B4A33), Color(0xFF7C5A40)), startY = y0, endY = y0 + topH),
            topLeft = Offset(0f, y0), size = Size(w, topH),
        )
        // Sisi depan
        drawRect(
            Brush.verticalGradient(listOf(Color(0xFF4A3021), Color(0xFF2E1D13)), startY = y0 + topH, endY = y0 + topH + frontH),
            topLeft = Offset(0f, y0 + topH), size = Size(w, frontH),
        )
        // Serat kayu
        val rnd = kotlin.random.Random(7)
        repeat(26) {
            val yy = y0 + rnd.nextFloat() * (topH + frontH)
            val a = 0.05f + rnd.nextFloat() * 0.07f
            val path = Path().apply {
                moveTo(0f, yy)
                var x = 0f
                while (x < w) {
                    x += 60f
                    lineTo(x, yy + kotlin.math.sin(x / (90f + it * 7f)) * 2.2f)
                }
            }
            drawPath(path, Color.Black.copy(alpha = a), style = Stroke(width = 1.2f))
        }
        // Garis sorot di tepi depan
        drawLine(Color.White.copy(alpha = 0.18f), Offset(0f, y0 + topH), Offset(w, y0 + topH), strokeWidth = 1.5f)
        drawLine(Color.White.copy(alpha = 0.08f), Offset(0f, y0), Offset(w, y0), strokeWidth = 1f)
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

