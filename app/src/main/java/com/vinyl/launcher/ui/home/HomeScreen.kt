package com.vinyl.launcher.ui.home

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vinyl.launcher.data.HomeSettings
import com.vinyl.launcher.data.TurntablePosition
import com.vinyl.launcher.data.WeatherSettings
import com.vinyl.launcher.data.WidgetConfig
import com.vinyl.launcher.data.WidgetType
import com.vinyl.launcher.media.NowPlaying
import com.vinyl.launcher.ui.components.BackgroundLayer
import com.vinyl.launcher.ui.components.BatteryIndicator
import com.vinyl.launcher.ui.components.ClockView
import com.vinyl.launcher.ui.components.TURNTABLE_ASPECT
import com.vinyl.launcher.ui.components.Turntable
import com.vinyl.launcher.ui.components.TurntableStyle
import com.vinyl.launcher.ui.components.TwoMonthCalendar
import com.vinyl.launcher.ui.components.VinylDock
import com.vinyl.launcher.ui.components.WeatherWidget
import com.vinyl.launcher.ui.components.rememberNow
import com.vinyl.launcher.ui.drawer.AppDrawer
import com.vinyl.launcher.ui.settings.AppPickerDialog
import com.vinyl.launcher.ui.theme.darken
import com.vinyl.launcher.ui.theme.horizontal
import com.vinyl.launcher.ui.theme.toAlignment
import com.vinyl.launcher.ui.theme.toColor
import com.vinyl.launcher.ui.theme.toComposeColor
import com.vinyl.launcher.util.SystemActions
import kotlin.math.roundToInt

/** Warna netral saat tidak ada musik. */
private val NeutralBg = Color(0xFF1B1B1F)
private val NeutralLabel = Color(0xFF8A8780)

@Composable
fun HomeScreen(vm: HomeViewModel) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val home by vm.home.collectAsStateWithLifecycle()
    val draft by vm.draft.collectAsStateWithLifecycle()
    val np by vm.nowPlaying.collectAsStateWithLifecycle()
    val drawerOpen by vm.drawerOpen.collectAsStateWithLifecycle()
    val visibleApps by vm.visibleApps.collectAsStateWithLifecycle()
    val allApps by vm.allApps.collectAsStateWithLifecycle()
    val dockApps by vm.dockApps.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val editing = draft != null
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Warna dari cover album (Palette); netral saat tidak ada musik
    val dominant = if (np.hasTrack) np.dominantColor?.toComposeColor() ?: NeutralBg else NeutralBg
    val accent = if (np.hasTrack) np.accentColor?.toComposeColor() ?: NeutralLabel else NeutralLabel
    val bgColor = dominant.darken(0.12f)

    BackHandler(enabled = drawerOpen || editing) {
        if (drawerOpen) vm.closeDrawer() else vm.cancelEdit()
    }

    val gestures by rememberUpdatedState(settings.gestures)
    var dockReplaceIndex by remember { mutableIntStateOf(-1) }

    Box(Modifier.fillMaxSize()) {
        BackgroundLayer(home.background, bgColor)

        // Area konten; saat edit, menyusut agar pratinjau tidak tertutup panel editor
        Box(
            Modifier
                .fillMaxSize()
                .padding(
                    end = if (editing && landscape) EDITOR_WIDTH else 0.dp,
                    bottom = if (editing && !landscape) EDITOR_HEIGHT_PORTRAIT else 0.dp,
                )
                .windowInsetsPadding(WindowInsets.safeDrawing) // ruang aman untuk taskbar & status bar
                .pointerInput(editing) {
                    if (editing) return@pointerInput
                    detectTapGestures(
                        onDoubleTap = { SystemActions.perform(context, gestures.doubleTap) { vm.openDrawer() } },
                        onLongPress = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            vm.startEdit()
                        },
                    )
                }
                .pointerInput(editing) {
                    if (editing) return@pointerInput
                    var total = 0f
                    detectVerticalDragGestures(
                        onDragStart = { total = 0f },
                        onDragEnd = {
                            when {
                                total < -120f -> SystemActions.perform(context, gestures.swipeUp) { vm.openDrawer() }
                                total > 120f -> SystemActions.perform(context, gestures.swipeDown) { vm.openDrawer() }
                            }
                        },
                    ) { change, dy -> change.consume(); total += dy }
                }
        ) {
            HomeContent(
                home = home,
                np = np,
                accent = accent,
                dominant = dominant,
                weather = settings.weather,
                editing = editing,
                onWidgetChange = { updated -> vm.updateDraft { h -> h.copy(widgets = h.widgets.map { if (it.id == updated.id) updated else it }) } },
                vm = vm,
            )

            // Dock vinyl cadangan (tanpa panel latar)
            if (settings.dock.enabled && dockApps.isNotEmpty() && !editing) {
                VinylDock(
                    apps = dockApps,
                    onLaunch = { vm.launch(it) },
                    onReplace = { dockReplaceIndex = it },
                    onRemove = { vm.removeDock(it) },
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp),
                )
            }
        }

        // App drawer
        AnimatedVisibility(
            visible = drawerOpen,
            enter = slideInVertically(tween(320)) { it / 3 } + fadeIn(tween(250)),
            exit = slideOutVertically(tween(260)) { it / 3 } + fadeOut(tween(200)),
        ) {
            AppDrawer(
                apps = visibleApps,
                tint = bgColor,
                dockEnabled = settings.dock.enabled,
                dockKeys = settings.dock.apps,
                onLaunch = vm::launch,
                onInfo = vm::appInfo,
                onUninstall = vm::uninstall,
                onAddToDock = { vm.addToDock(it) },
                onHide = { vm.hideApp(it) },
                onClose = vm::closeDrawer,
            )
        }

        // Panel editor (kanan di landscape, bawah di portrait)
        AnimatedVisibility(
            visible = editing,
            modifier = Modifier.align(if (landscape) Alignment.CenterEnd else Alignment.BottomCenter),
            enter = if (landscape) slideInHorizontally { it } else slideInVertically { it },
            exit = if (landscape) slideOutHorizontally { it } else slideOutVertically { it },
        ) {
            HomeEditorPanel(vm = vm, landscape = landscape, apps = allApps)
        }
    }

    if (dockReplaceIndex >= 0) {
        AppPickerDialog(visibleApps, "Ganti aplikasi dock", onDismiss = { dockReplaceIndex = -1 }) {
            vm.replaceDock(dockReplaceIndex, it); dockReplaceIndex = -1
        }
    }
}

/** Jam, turntable & widget. Dipisah agar mudah dibaca. */
@Composable
private fun HomeContent(
    home: HomeSettings,
    np: NowPlaying,
    accent: Color,
    dominant: Color,
    weather: WeatherSettings,
    editing: Boolean,
    onWidgetChange: (WidgetConfig) -> Unit,
    vm: HomeViewModel,
) {
    val now by rememberNow()
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val maxW = maxWidth
        val maxH = maxHeight

        // ----- Turntable -----
        val baseWidth = minOf(maxW * 0.94f, maxH * 0.80f * TURNTABLE_ASPECT)
        val ttWidth = baseWidth * home.turntableSize
        val ttHeight = ttWidth / TURNTABLE_ASPECT
        val align = when (home.turntablePosition) {
            TurntablePosition.LEFT -> Alignment.CenterStart
            TurntablePosition.CENTER -> Alignment.Center
            TurntablePosition.RIGHT -> Alignment.CenterEnd
        }
        Turntable(
            nowPlaying = np,
            style = TurntableStyle(
                vinylStyle = home.vinylStyle,
                tonearmStyle = home.tonearmStyle,
                coverVisible = home.coverVisible,
                coverRounded = home.coverRounded,
                coverShadow = home.coverShadow,
                rpm = home.rpm,
            ),
            labelColor = accent,
            discTint = dominant,
            interactive = !editing,
            onTogglePlay = vm::togglePlay,
            onNext = vm::next,
            onPrevious = vm::previous,
            onSeek = vm::seek,
            onOpenMusic = vm::openMusic,
            modifier = Modifier
                .align(align)
                .padding(horizontal = 24.dp)
                .size(ttWidth, ttHeight),
        )

        // ----- Jam & tanggal (kecil, default pojok kiri atas) -----
        if (home.clock.visible) {
            ClockView(
                settings = home.clock,
                now = now,
                horizontalAlignment = home.clock.anchor.horizontal(),
                modifier = Modifier.align(home.clock.anchor.toAlignment()).padding(28.dp),
            )
        }

        // ----- Widget tambahan -----
        val wPx = constraints.maxWidth
        val hPx = constraints.maxHeight
        home.widgets.forEach { w ->
            HomeWidget(w, wPx, hPx, editing, accent, weather, home.clock.color.toColor(), onWidgetChange)
        }
    }
}

/**
 * Widget yang bisa diseret & diubah ukurannya di mode edit.
 * Posisi disimpan sebagai fraksi layar agar tetap pas setelah rotasi.
 */
@Composable
private fun HomeWidget(
    config: WidgetConfig,
    areaW: Int,
    areaH: Int,
    editing: Boolean,
    accent: Color,
    weather: WeatherSettings,
    textColor: Color,
    onChange: (WidgetConfig) -> Unit,
) {
    val cfg by rememberUpdatedState(config)
    var size by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current
    val x = (config.x * areaW).roundToInt().coerceIn(0, (areaW - size.width).coerceAtLeast(0))
    val y = (config.y * areaH).roundToInt().coerceIn(0, (areaH - size.height).coerceAtLeast(0))

    Box(
        Modifier
            .offset { IntOffset(x, y) }
            .onSizeChanged { size = it }
            .then(
                if (editing) Modifier
                    .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .pointerInput(areaW, areaH) {
                        detectDragGestures { change, drag ->
                            change.consume()
                            onChange(
                                cfg.copy(
                                    x = (cfg.x + drag.x / areaW).coerceIn(0f, 0.98f),
                                    y = (cfg.y + drag.y / areaH).coerceIn(0f, 0.98f),
                                )
                            )
                        }
                    }
                else Modifier
            )
            .padding(12.dp)
    ) {
        when (config.type) {
            WidgetType.CALENDAR -> TwoMonthCalendar(
                today = java.time.LocalDate.now(),
                showEvents = false,
                color = textColor,
                accent = accent,
                cell = (22 * config.scale).dp,
            )
            WidgetType.WEATHER -> WeatherWidget(weather, textColor, scale = config.scale)
            WidgetType.BATTERY -> BatteryIndicator(textColor, scale = config.scale)
        }
        if (editing) {
            // Pegangan ubah ukuran di sudut kanan bawah
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .offset(14.dp, 14.dp)
                    .size(22.dp)
                    .background(Color.White, CircleShape)
                    .pointerInput(Unit) {
                        detectDragGestures { change, drag: Offset ->
                            change.consume()
                            val delta = (drag.x + drag.y) / with(density) { 300.dp.toPx() }
                            onChange(cfg.copy(scale = (cfg.scale + delta).coerceIn(0.6f, 2.2f)))
                        }
                    }
            )
        }
    }
}

/** Lebar panel editor di landscape & tinggi di portrait. */
val EDITOR_WIDTH = 400.dp
val EDITOR_HEIGHT_PORTRAIT = 440.dp
