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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.layout.widthIn
import androidx.compose.animation.animateColorAsState
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
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
 * "Rak piringan": lapisan layar penuh dengan tiga tab (kontrol segmen ala iOS/macOS)
 * - Rak: deretan sampul cover-flow di atas papan kayu, piringan hitam menyembul dari sampul,
 *   plus lajur "Baru diputar" di bawahnya
 * - Riwayat: daftar lagu yang pernah diputar, dikelompokkan per hari
 * - Koleksi: statistik & semua lagu yang pernah didengarkan
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
                Modifier.fillMaxWidth().padding(start = 36.dp, end = 20.dp, top = 22.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Rak piringan", style = MaterialTheme.typography.displaySmall, color = TextPrimary)
                    Text(
                        "${history.size} lagu dalam koleksi",
                        style = MaterialTheme.typography.bodyMedium, color = TextSecondary,
                    )
                }
                Box(
                    Modifier.size(44.dp).clip(CircleShape).glass(CircleShape).clickable(onClick = onClose),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Close, "Tutup", tint = Color.White) }
            }
            Box(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 6.dp), contentAlignment = Alignment.Center) {
                Segmented(listOf("Rak", "Riwayat", "Koleksi"), tab) { tab = it }
            }
            when (tab) {
                0 -> RecordShelf(np, queue, history)
                1 -> HistoryList(history, onClearHistory)
                else -> Collection(history, onClearHistory)
            }
        }
    }
}

/** Kontrol segmen kapsul ala iOS: pil putih bergeser ke tab terpilih. */
@Composable
private fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier.glass(RoundedCornerShape(50), fillAlpha = 0.10f).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEachIndexed { i, t ->
            val sel = i == selected
            val bg by animateColorAsState(if (sel) Color.White.copy(alpha = 0.94f) else Color.Transparent, label = "seg")
            Text(
                t,
                style = MaterialTheme.typography.labelLarge,
                color = if (sel) Color(0xFF111113) else Color.White.copy(alpha = 0.85f),
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(bg)
                    .clickable { onSelect(i) }
                    .padding(horizontal = 26.dp, vertical = 9.dp),
            )
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
        val showStrip = maxHeight > 560.dp && history.isNotEmpty()
        val sleeve = minOf(maxHeight * (if (showStrip) 0.40f else 0.50f), maxWidth * 0.28f, 320.dp)
        val step = sleeve * 0.66f
        val sidePad = (maxWidth - step) / 2
        val focused = items.getOrNull(pager.currentPage)

        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.weight(0.15f))
            Box(Modifier.fillMaxWidth().height(sleeve + 76.dp)) {
                // Papan kayu (digambar di belakang sampul)
                Shelf(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(76.dp))
                // Cahaya lembut di bawah sampul yang sedang dipilih
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 54.dp)
                        .size(width = sleeve * 1.3f, height = 26.dp)
                        .background(
                            Brush.radialGradient(listOf(Accent.copy(alpha = 0.30f), Color.Transparent)),
                            RoundedCornerShape(50),
                        ),
                )
                HorizontalPager(
                    state = pager,
                    pageSize = PageSize.Fixed(step),
                    contentPadding = PaddingValues(horizontal = sidePad),
                    beyondViewportPageCount = 3,
                    modifier = Modifier.fillMaxWidth().height(sleeve + 36.dp).align(Alignment.TopCenter),
                    verticalAlignment = Alignment.Bottom,
                ) { page ->
                    val item = items[page]
                    val img = item.art ?: rememberImage(item.bitmap, item.source)
                    val offset = (pager.currentPage - page) + pager.currentPageOffsetFraction
                    val abs = kotlin.math.abs(offset).coerceAtMost(2f)
                    // Piringan menyembul lebih jauh dari sampul yang sedang di tengah
                    val peek = 0.10f + 0.20f * (1f - abs.coerceAtMost(1f))
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
                        DiscPeek(peek = peek, modifier = Modifier.requiredSize(sleeve))
                        Sleeve(img, null, highlight = item.relation == 0)
                    }
                }
            }
            // Info sampul yang sedang di tengah (kartu kaca ala iOS)
            Spacer(Modifier.height(16.dp))
            if (focused != null) {
                val (label, color) = when {
                    focused.relation == 0 -> "SEDANG DIPUTAR" to Accent
                    focused.relation < 0 -> "SEBELUMNYA" to TextTertiary
                    else -> "BERIKUTNYA" to TextTertiary
                }
                Row(
                    Modifier.widthIn(max = 560.dp).glass(RoundedCornerShape(26.dp)).padding(horizontal = 22.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (focused.relation == 0) {
                                Icon(Icons.Rounded.GraphicEq, null, tint = Accent, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(6.dp))
                            }
                            Text(label, color = color, style = MaterialTheme.typography.labelMedium)
                            Text("   ${pager.currentPage + 1} / ${items.size}", color = TextTertiary, style = MaterialTheme.typography.labelMedium)
                        }
                        Text(focused.title, style = MaterialTheme.typography.titleLarge, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
                        Text(focused.subtitle, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    if (focused.relation != 0 && focused.play != null) {
                        Spacer(Modifier.width(14.dp))
                        Row(
                            Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color.White.copy(alpha = 0.94f))
                                .clickable { focused.play.invoke() }
                                .padding(horizontal = 18.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Rounded.PlayArrow, null, tint = Color(0xFF111113), modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Putar", color = Color(0xFF111113), style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
            // Lajur riwayat: lagu yang baru diputar
            if (showStrip) {
                Spacer(Modifier.height(18.dp))
                Text(
                    "BARU DIPUTAR",
                    style = MaterialTheme.typography.labelMedium, color = TextSecondary,
                    modifier = Modifier.align(Alignment.Start).padding(start = 40.dp, bottom = 8.dp),
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 36.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    items(history.take(14), key = { it.key }) { r ->
                        val img = rememberImage(null, r.artFile)
                        Column(
                            Modifier.width(92.dp).clickable { playFromSearch(context, r) },
                        ) {
                            Box(Modifier.size(92.dp)) {
                                DiscPeek(peek = 0.12f, modifier = Modifier.size(92.dp))
                                Sleeve(img, 92.dp, highlight = false)
                            }
                            Text(r.title, style = MaterialTheme.typography.labelMedium, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.weight(0.25f))
        }
    }
}

/** Piringan hitam yang menyembul dari sisi kanan sampul (digambar di belakang sampul). */
@Composable
private fun DiscPeek(peek: Float, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val d = size.minDimension * 0.96f
        val r = d / 2f
        val c = Offset(size.width / 2f + size.width * peek, size.height / 2f)
        drawCircle(Color(0xFF0B0B0D), r, c)
        // alur
        var rr = r * 0.34f
        while (rr < r * 0.97f) {
            drawCircle(Color.White.copy(alpha = 0.045f), rr, c, style = Stroke(width = 1f))
            rr += r * 0.045f
        }
        // kilau
        drawCircle(
            Brush.sweepGradient(
                listOf(Color.Transparent, Color.White.copy(alpha = 0.10f), Color.Transparent, Color.White.copy(alpha = 0.08f), Color.Transparent),
                center = c,
            ),
            r, c,
        )
        // label & lubang tengah
        drawCircle(Accent, r * 0.30f, c)
        drawCircle(Color.Black.copy(alpha = 0.25f), r * 0.30f, c, style = Stroke(width = 2f))
        drawCircle(Color(0xFF0B0B0D), r * 0.04f, c)
        drawCircle(Color.White.copy(alpha = 0.12f), r, c, style = Stroke(width = 1.2f))
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

// ======================= Riwayat =======================

private data class HistoryEntry(val record: PlayRecord, val at: Long)

private fun dayLabel(at: Long): String {
    val zone = java.time.ZoneId.systemDefault()
    val d = java.time.Instant.ofEpochMilli(at).atZone(zone).toLocalDate()
    val today = java.time.LocalDate.now(zone)
    return when (d) {
        today -> "Hari ini"
        today.minusDays(1) -> "Kemarin"
        else -> java.time.format.DateTimeFormatter.ofPattern("EEEE, d MMMM", java.util.Locale.forLanguageTag("id-ID")).format(d)
    }
}

private fun clockLabel(at: Long): String =
    java.time.format.DateTimeFormatter.ofPattern("HH:mm").format(
        java.time.Instant.ofEpochMilli(at).atZone(java.time.ZoneId.systemDefault()),
    )

/** Riwayat lagu yang diputar, terbaru di atas, dikelompokkan per hari (gaya daftar iOS/One UI). */
@Composable
private fun HistoryList(history: List<PlayRecord>, onClear: () -> Unit) {
    val context = LocalContext.current
    var confirmClear by remember { mutableStateOf(false) }
    val entries = remember(history) {
        history.flatMap { r ->
            val times = r.playTimes.ifEmpty { listOf(r.lastPlayed) }
            times.map { HistoryEntry(r, it) }
        }.sortedByDescending { it.at }.take(300)
    }
    if (entries.isEmpty()) {
        EmptyState("Belum ada riwayat", "Lagu yang Anda dengarkan lebih dari 30 detik akan tercatat di sini.")
        return
    }
    val groups = remember(entries) { entries.groupBy { dayLabel(it.at) } }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.widthIn(max = 760.dp).fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 28.dp, vertical = 12.dp),
        ) {
            groups.forEach { (label, list) ->
                item(key = "h_$label") {
                    Row(Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 8.dp, start = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(label.uppercase(), style = MaterialTheme.typography.labelMedium, color = TextSecondary, modifier = Modifier.weight(1f))
                        if (label == groups.keys.first()) TextButton(onClick = { confirmClear = true }) { Text("Hapus riwayat", color = TextSecondary) }
                    }
                }
                item(key = "g_$label") {
                    Column(Modifier.fillMaxWidth().glass(RoundedCornerShape(22.dp))) {
                        list.forEachIndexed { i, e ->
                            val img = rememberImage(null, e.record.artFile)
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { playFromSearch(context, e.record) }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(Modifier.size(58.dp)) {
                                    DiscPeek(peek = 0.13f, modifier = Modifier.size(58.dp))
                                    Sleeve(img, 58.dp, highlight = false)
                                }
                                Spacer(Modifier.width(18.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(e.record.title, style = MaterialTheme.typography.titleSmall, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(e.record.artist, style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                Text(clockLabel(e.at), style = MaterialTheme.typography.labelMedium, color = TextTertiary)
                                Spacer(Modifier.width(12.dp))
                                Icon(Icons.Rounded.PlayArrow, "Putar ulang", tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(24.dp))
                            }
                            if (i < list.lastIndex) Box(Modifier.padding(start = 90.dp).fillMaxWidth().height(0.6.dp).background(Color.White.copy(alpha = 0.08f)))
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
    if (confirmClear) AlertDialog(
        onDismissRequest = { confirmClear = false },
        title = { Text("Hapus semua riwayat?") },
        text = { Text("Riwayat, koleksi dan statistik akan dikosongkan.") },
        confirmButton = { TextButton(onClick = { onClear(); confirmClear = false }) { Text("Hapus") } },
        dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Batal") } },
    )
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

