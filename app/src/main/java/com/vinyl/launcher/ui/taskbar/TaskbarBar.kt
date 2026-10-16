package com.vinyl.launcher.ui.taskbar

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vinyl.launcher.data.AppInfo
import com.vinyl.launcher.data.AppsRepository
import com.vinyl.launcher.data.DockPosition
import com.vinyl.launcher.data.SettingsRepository
import com.vinyl.launcher.media.MediaRepository
import com.vinyl.launcher.ui.home.HomeActivity
import com.vinyl.launcher.service.TaskbarState
import com.vinyl.launcher.ui.settings.SettingsActivity
import kotlinx.coroutines.flow.StateFlow

private val BarShape = RoundedCornerShape(26.dp)

/**
 * Isi Taskbar Vinyl: kapsul kaca gelap berisi Home, semua aplikasi, favorit,
 * aplikasi terakhir, dan kartu kecil lagu yang sedang diputar.
 * Bisa diciutkan menjadi pegangan kecil agar tidak menutupi konten aplikasi.
 */
@Composable
fun TaskbarBar(recents: StateFlow<List<AppInfo>>) {
    val context = LocalContext.current
    val settings by SettingsRepository.get(context).settings.collectAsStateWithLifecycle()
    val apps by AppsRepository.get(context).apps.collectAsStateWithLifecycle()
    val recentApps by recents.collectAsStateWithLifecycle()
    val np by MediaRepository.state.collectAsStateWithLifecycle()
    var collapsed by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current

    val favorites = settings.dock.apps.mapNotNull { key -> apps.firstOrNull { it.key == key } }
    val favPkgs = favorites.map { it.packageName }.toSet()
    val recentsShown = if (settings.dock.showRecents) recentApps.filter { it.packageName !in favPkgs }.take(3) else emptyList()

    val vertical = settings.dock.position == DockPosition.LEFT || settings.dock.position == DockPosition.RIGHT

    // Tanpa AnimatedContent/bayangan: jendela overlay ukurannya mengikuti isi, sehingga animasi ukuran
    // dan bayangan yang meluber keluar jendela terlihat sebagai potongan/bayangan "glitch".
    if (collapsed) {
        // Pegangan kecil: ketuk untuk membuka kembali
        Box(
            Modifier
                .padding(4.dp)
                .size(width = if (vertical) 24.dp else 120.dp, height = if (vertical) 120.dp else 24.dp)
                .clip(RoundedCornerShape(50))
                .clickable { collapsed = false }
                .semantics { contentDescription = "Buka taskbar" },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .size(width = if (vertical) 5.dp else 64.dp, height = if (vertical) 64.dp else 5.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.6f)),
            )
        }
    } else {
        val content: @Composable () -> Unit = {
            BarIcon(Icons.Rounded.Home, "Layar utama") { TaskbarState.setAppPanel(false); goHome(context, openDrawer = false) }
            BarIcon(Icons.Rounded.Apps, "Semua aplikasi") { TaskbarState.toggleAppPanel() }

            if (favorites.isNotEmpty()) {
                Divider(vertical)
                favorites.forEach { app ->
                    AppButton(app, dim = false, onLongClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        openTaskbarSettings(context)
                    }) { AppsRepository.get(context).launch(app) }
                }
            }
            if (recentsShown.isNotEmpty()) {
                Divider(vertical)
                recentsShown.forEach { app ->
                    AppButton(app, dim = true, onLongClick = {}) { AppsRepository.get(context).launch(app) }
                }
            }
            if (favorites.isEmpty() && recentsShown.isEmpty()) {
                BarIcon(Icons.Rounded.Settings, "Tambah favorit di Pengaturan") { openTaskbarSettings(context) }
            }
            if (settings.dock.showNowPlaying && np.hasTrack) {
                Divider(vertical)
                NowPlayingChip(context, compact = vertical)
            }
            Divider(vertical)
            BarIcon(Icons.Rounded.ExpandMore, "Ciutkan taskbar", size = 22) { TaskbarState.setAppPanel(false); collapsed = true }
        }
        val barModifier = Modifier
            .padding(4.dp)
            .clip(BarShape)
            .background(Color(0xF2151517))
            .border(0.8.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.22f), Color.White.copy(alpha = 0.06f))), BarShape)
            .padding(horizontal = 10.dp, vertical = 8.dp)
        if (vertical) {
            Column(barModifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) { content() }
        } else {
            Row(barModifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) { content() }
        }
    }
}

@Composable
private fun Divider(vertical: Boolean) {
    Box(
        Modifier
            .padding(if (vertical) PaddingValues(vertical = 4.dp) else PaddingValues(horizontal = 4.dp))
            .size(width = if (vertical) 28.dp else 1.dp, height = if (vertical) 1.dp else 28.dp)
            .background(Color.White.copy(alpha = 0.14f)),
    )
}

@Composable
private fun BarIcon(icon: ImageVector, desc: String, size: Int = 26, onClick: () -> Unit) {
    Box(
        Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, desc, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(size.dp)) }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppButton(app: AppInfo, dim: Boolean, onLongClick: () -> Unit, onClick: () -> Unit) {
    Box(
        Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .semantics { contentDescription = app.label },
        contentAlignment = Alignment.Center,
    ) {
        Image(app.icon, null, Modifier.size(40.dp).alpha(if (dim) 0.78f else 1f))
    }
}

/** Kartu kecil lagu: cover, judul, tombol putar/jeda. */
@Composable
private fun NowPlayingChip(context: Context, compact: Boolean) {
    val np by MediaRepository.state.collectAsStateWithLifecycle()
    Row(
        Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.07f))
            .clickable { MediaRepository.openMusicApp(context) }
            .padding(start = 6.dp, end = 2.dp, top = 5.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(9.dp)).background(Color.White.copy(alpha = 0.08f)), contentAlignment = Alignment.Center) {
            val art = np.art
            if (art != null) Image(art, null, contentScale = ContentScale.Crop, modifier = Modifier.size(36.dp))
            else Icon(Icons.Rounded.MusicNote, null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
        }
        if (!compact) {
            Spacer(Modifier.width(10.dp))
            Column(Modifier.widthIn(max = 150.dp)) {
                Text(np.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(np.artist, color = Color.White.copy(alpha = 0.55f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(2.dp))
        }
        Box(
            Modifier.size(40.dp).clip(CircleShape).clickable { MediaRepository.togglePlayPause("Taskbar") },
            contentAlignment = Alignment.Center,
        ) {
            Icon(if (np.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, "Putar/jeda", tint = Color.White, modifier = Modifier.size(24.dp))
        }
    }
}

private fun goHome(context: Context, openDrawer: Boolean) {
    val intent = if (openDrawer) {
        Intent(context, HomeActivity::class.java).putExtra(HomeActivity.EXTRA_OPEN_DRAWER, true)
    } else {
        Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
    }
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
    runCatching { context.startActivity(intent) }
}

private fun openTaskbarSettings(context: Context) {
    runCatching {
        context.startActivity(Intent(context, SettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
