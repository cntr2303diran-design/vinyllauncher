package com.vinyl.launcher.ui.drawer

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vinyl.launcher.data.AppInfo
import com.vinyl.launcher.ui.theme.PaperCream
import com.vinyl.launcher.ui.components.AmbientBackdrop
import com.vinyl.launcher.ui.components.glass
import androidx.compose.material3.MaterialTheme

/**
 * App drawer: grid abjad dengan pencarian di atas.
 * Ditutup dengan tombol back, tombol home, atau menarik ke bawah saat daftar di posisi paling atas.
 */
@Composable
fun AppDrawer(
    apps: List<AppInfo>,
    tint: Color,
    np: com.vinyl.launcher.media.NowPlaying,
    dockEnabled: Boolean,
    dockKeys: List<String>,
    onLaunch: (AppInfo) -> Unit,
    onInfo: (AppInfo) -> Unit,
    onUninstall: (AppInfo) -> Unit,
    onAddToDock: (AppInfo) -> Unit,
    onHide: (AppInfo) -> Unit,
    onClose: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val focus = LocalFocusManager.current
    val gridState = rememberLazyGridState()
    var pull by remember { mutableFloatStateOf(0f) }

    // Tarik ke bawah di puncak daftar → tutup drawer
    val closeOnPull = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (available.y > 0f && source == NestedScrollSource.UserInput) {
                    pull += available.y
                    if (pull > 220f) { pull = 0f; onClose() }
                } else if (available.y < 0f) pull = 0f
                return Offset.Zero
            }
        }
    }

    val filtered = remember(apps, query) {
        if (query.isBlank()) apps else apps.filter { it.label.contains(query.trim(), ignoreCase = true) }
    }

    Box(Modifier.fillMaxSize()) {
        // Latar ala App Library iOS: ambient cover yang lebih gelap
        AmbientBackdrop(np.art.takeIf { np.hasTrack }, np.swatches, tint, animate = false, dim = 0.62f)
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            // Pegangan + kolom pencarian (juga bisa ditarik ke bawah untuk menutup)
            Column(
                Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        var total = 0f
                        detectVerticalDragGestures(
                            onDragEnd = { if (total > 120f) onClose(); total = 0f },
                        ) { _, dy -> total += dy }
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier.padding(top = 10.dp).size(width = 40.dp, height = 5.dp)
                        .background(PaperCream.copy(alpha = 0.3f), RoundedCornerShape(50))
                )
                Text(
                    "Aplikasi",
                    style = MaterialTheme.typography.displaySmall,
                    color = PaperCream,
                    modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth().padding(start = 36.dp, top = 22.dp),
                )
                SearchField(query, { query = it }, Modifier.padding(top = 16.dp, bottom = 14.dp).widthIn(max = 640.dp).fillMaxWidth().padding(horizontal = 32.dp))
            }
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 112.dp),
                state = gridState,
                contentPadding = PaddingValues(horizontal = 32.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize().nestedScroll(closeOnPull),
            ) {
                items(filtered, key = { it.key }) { app ->
                    AppTile(
                        app = app,
                        canAddToDock = dockEnabled && app.key !in dockKeys,
                        onClick = { focus.clearFocus(); onLaunch(app) },
                        onInfo = { onInfo(app) },
                        onUninstall = { onUninstall(app) },
                        onAddToDock = { onAddToDock(app) },
                        onHide = { onHide(app) },
                    )
                }
                if (filtered.isEmpty()) item {
                    Text("Tidak ada aplikasi", color = PaperCream.copy(alpha = 0.6f), modifier = Modifier.padding(24.dp))
                }
            }
        }
    }
}

@Composable
private fun SearchField(value: String, onChange: (String) -> Unit, modifier: Modifier) {
    Box(
        modifier
            .glass(RoundedCornerShape(50), fillAlpha = 0.12f)
            .padding(horizontal = 18.dp, vertical = 13.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Search, null, tint = PaperCream.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
            Spacer(Modifier.size(10.dp))
            Box {
                if (value.isEmpty()) Text("Cari aplikasi", color = PaperCream.copy(alpha = 0.45f), fontSize = 16.sp)
                BasicTextField(
                    value = value,
                    onValueChange = onChange,
                    singleLine = true,
                    textStyle = TextStyle(color = PaperCream, fontSize = 16.sp),
                    cursorBrush = SolidColor(PaperCream),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppTile(
    app: AppInfo,
    canAddToDock: Boolean,
    onClick: () -> Unit,
    onInfo: () -> Unit,
    onUninstall: () -> Unit,
    onAddToDock: () -> Unit,
    onHide: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    Box {
        Column(
            Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { haptics.performHapticFeedback(HapticFeedbackType.LongPress); menu = true },
                )
                .padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(app.icon, contentDescription = null, modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(9.dp))
            Text(
                app.label,
                color = PaperCream.copy(alpha = 0.92f),
                fontSize = 13.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 6.dp),
            )
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(text = { Text("Info aplikasi") }, onClick = { menu = false; onInfo() })
            DropdownMenuItem(text = { Text("Uninstall") }, onClick = { menu = false; onUninstall() })
            if (canAddToDock) DropdownMenuItem(text = { Text("Tambahkan ke dock") }, onClick = { menu = false; onAddToDock() })
            DropdownMenuItem(text = { Text("Sembunyikan aplikasi") }, onClick = { menu = false; onHide() })
        }
    }
}
