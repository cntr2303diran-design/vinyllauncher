package com.vinyl.launcher.ui.cover

import android.Manifest
import android.content.res.Configuration
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vinyl.launcher.data.Anchor
import com.vinyl.launcher.data.ClockType
import com.vinyl.launcher.data.CoverSettings
import com.vinyl.launcher.data.VinylStyle
import com.vinyl.launcher.media.NowPlaying
import com.vinyl.launcher.ui.components.BackgroundLayer
import com.vinyl.launcher.ui.components.BatteryIndicator
import com.vinyl.launcher.ui.components.ClockView
import com.vinyl.launcher.ui.components.DateText
import com.vinyl.launcher.ui.components.SongInfo
import com.vinyl.launcher.ui.components.TwoMonthCalendar
import com.vinyl.launcher.ui.components.VinylDisc
import com.vinyl.launcher.ui.components.VinylLabel
import com.vinyl.launcher.ui.components.rememberNow
import com.vinyl.launcher.ui.components.timeParts
import com.vinyl.launcher.ui.settings.CoverStyleControls
import com.vinyl.launcher.ui.theme.PaperCream
import com.vinyl.launcher.ui.theme.horizontal
import com.vinyl.launcher.ui.theme.toAlignment
import com.vinyl.launcher.ui.theme.toColor
import com.vinyl.launcher.ui.theme.toComposeColor
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.min

private val EDITOR_WIDTH = 400.dp

/** Elemen info yang ditempatkan di area kanan (landscape) / bawah (portrait). */
private enum class CoverItem { CLOCK, DATE, CALENDAR, SONG, BATTERY }

@Composable
fun CoverScreen(
    vm: CoverViewModel,
    onUnlock: (onCancelled: () -> Unit) -> Unit,
    onRequestEdit: () -> Unit,
) {
    val cover by vm.cover.collectAsStateWithLifecycle()
    val draft by vm.draft.collectAsStateWithLifecycle()
    val np by vm.nowPlaying.collectAsStateWithLifecycle()
    val editing = draft != null
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val haptics = LocalHapticFeedback.current
    val now by rememberNow()

    val albumColor = np.dominantColor?.takeIf { np.hasTrack }?.toComposeColor() ?: Color(0xFF2A2A30)
    val accent = np.accentColor?.takeIf { np.hasTrack }?.toComposeColor() ?: Color(0xFF8A8780)

    Box(Modifier.fillMaxSize()) {
        BackgroundLayer(cover.background, albumColor, glow = true)

        Box(
            Modifier
                .fillMaxSize()
                .padding(end = if (editing && landscape) EDITOR_WIDTH else 0.dp, bottom = if (editing && !landscape) 420.dp else 0.dp)
                .pointerInput(editing) {
                    if (editing) return@pointerInput
                    detectTapGestures(onLongPress = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onRequestEdit()
                    })
                }
                .padding(32.dp)
        ) {
            if (landscape) {
                Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                    BoxWithConstraints(Modifier.weight(0.46f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                        CoverVinyl(np, cover, accent, albumColor, min(maxWidth.value, maxHeight.value * 0.9f).dp, editing, onUnlock)
                    }
                    InfoArea(cover, np, now, accent, Modifier.weight(0.54f).fillMaxHeight().padding(start = 24.dp))
                }
            } else {
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                    BoxWithConstraints(Modifier.fillMaxWidth().weight(0.48f), contentAlignment = Alignment.Center) {
                        CoverVinyl(np, cover, accent, albumColor, min(maxWidth.value * 0.85f, maxHeight.value).dp, editing, onUnlock)
                    }
                    InfoArea(cover, np, now, accent, Modifier.fillMaxWidth().weight(0.52f).padding(top = 24.dp))
                }
            }

            if (!editing) UnlockHint(Modifier.align(Alignment.BottomCenter))
        }

        // Editor dengan pratinjau langsung
        AnimatedVisibility(
            visible = editing,
            modifier = Modifier.align(if (landscape) Alignment.CenterEnd else Alignment.BottomCenter),
            enter = if (landscape) slideInHorizontally { it } else slideInVertically { it },
            exit = if (landscape) slideOutHorizontally { it } else slideOutVertically { it },
        ) {
            CoverEditorPanel(vm, landscape)
        }
    }
}

/** Petunjuk "Geser ke kanan untuk membuka" yang berdenyut pelan. */
@Composable
private fun UnlockHint(modifier: Modifier) {
    val pulse by rememberInfiniteTransition(label = "hint").animateFloat(
        0.35f, 0.8f, infiniteRepeatable(tween(1600), RepeatMode.Reverse), label = "a",
    )
    Text(
        "Geser ke kanan untuk membuka  →",
        color = PaperCream.copy(alpha = pulse),
        fontSize = 15.sp,
        letterSpacing = 1.sp,
        modifier = modifier,
    )
}

/**
 * Vinyl besar berlabel cover album. Berputar saat musik aktif.
 * Geser ke kanan melewati ambang → panggil [onUnlock]; jika dibatalkan, kembali ke posisi semula.
 */
@Composable
private fun CoverVinyl(
    np: NowPlaying,
    cover: CoverSettings,
    accent: Color,
    discTint: Color,
    size: Dp,
    editing: Boolean,
    onUnlock: (onCancelled: () -> Unit) -> Unit,
) {
    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var angle by remember { mutableFloatStateOf(0f) }
    var velocity by remember { mutableFloatStateOf(0f) }
    val playing = np.isPlaying && np.hasTrack
    val now by rememberNow()

    // Putaran 33⅓ RPM dengan akselerasi/deselerasi halus
    LaunchedEffect(playing) {
        var last = 0L
        val target = if (playing) 200f else 0f
        while (isActive) {
            withFrameNanos { t ->
                val dt = if (last == 0L) 0f else (t - last) / 1e9f
                last = t
                velocity = if (velocity < target) min(target, velocity + 250f * dt) else maxOf(target, velocity - 130f * dt)
                angle = (angle + velocity * dt) % 360f
            }
            if (!playing && velocity <= 0f) break
        }
    }

    // Jam melingkar ditulis di tepi vinyl
    val rimText = if (cover.clock.visible && cover.clock.type == ClockType.CIRCULAR) {
        val (hm, ampm) = timeParts(now, cover.clock.use24h)
        hm + (ampm?.let { " $it" } ?: "")
    } else null

    BoxWithConstraints(Modifier.size(size)) {
        val widthPx = constraints.maxWidth.toFloat()
        VinylDisc(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = offsetX.value
                    alpha = 1f - (offsetX.value / (widthPx * 1.6f)).coerceIn(0f, 0.7f)
                }
                .pointerInput(editing) {
                    if (editing) return@pointerInput
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            scope.launch {
                                if (offsetX.value > widthPx * 0.35f) {
                                    offsetX.animateTo(widthPx * 1.2f, tween(260))
                                    onUnlock { scope.launch { offsetX.animateTo(0f, spring(0.7f, 250f)) } }
                                } else offsetX.animateTo(0f, spring(0.6f, 300f))
                            }
                        },
                        onDragCancel = { scope.launch { offsetX.animateTo(0f) } },
                    ) { change, dx ->
                        change.consume()
                        scope.launch { offsetX.snapTo((offsetX.value + dx).coerceAtLeast(0f)) }
                    }
                },
            rotation = angle + offsetX.value * 0.25f, // ikut menggelinding saat digeser
            style = VinylStyle.CLASSIC,
            labelColor = accent,
            discTint = discTint,
            label = VinylLabel.Art(if (np.hasTrack) np.art else null),
            rimText = rimText,
            rimTextColor = cover.clock.color.toColor(),
        )
    }
}

@Composable
private fun InfoArea(cover: CoverSettings, np: NowPlaying, now: java.time.LocalDateTime, accent: Color, modifier: Modifier) {
    val clockOnRim = cover.clock.type == ClockType.CIRCULAR
    // Kelompokkan elemen per posisi agar yang berada di posisi sama tersusun rapi
    val items = buildList {
        if (cover.clock.visible && !clockOnRim) add(CoverItem.CLOCK to cover.clock.anchor)
        if (cover.date.visible) add(CoverItem.DATE to cover.date.anchor)
        if (cover.calendar.visible) add(CoverItem.CALENDAR to cover.calendar.anchor)
        if (cover.songInfo.visible) add(CoverItem.SONG to cover.songInfo.anchor)
        if (cover.battery.visible) add(CoverItem.BATTERY to cover.battery.anchor)
    }.groupBy({ it.second }, { it.first })

    val textColor = cover.clock.color.toColor()
    BoxWithConstraints(modifier) {
        val cell = min(30f, maxWidth.value / 16f).dp
        items.forEach { (anchor: Anchor, list) ->
            Column(
                Modifier.align(anchor.toAlignment()),
                horizontalAlignment = anchor.horizontal(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                list.forEach { item ->
                    when (item) {
                        CoverItem.CLOCK -> ClockView(
                            settings = cover.clock.copy(showDate = false),
                            now = now,
                            baseSize = 96.dp,
                            horizontalAlignment = anchor.horizontal(),
                        )
                        CoverItem.DATE -> DateText(now, cover.clock, baseSize = 96.dp)
                        CoverItem.CALENDAR -> TwoMonthCalendar(
                            today = LocalDate.from(now),
                            showEvents = cover.calendarEvents,
                            color = textColor,
                            accent = accent,
                            cell = cell,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                        CoverItem.SONG -> SongInfo(np.title, np.artist, textColor, alignEnd = anchor.horizontal() == Alignment.End)
                        CoverItem.BATTERY -> BatteryIndicator(textColor)
                    }
                }
            }
        }
    }
}

@Composable
private fun CoverEditorPanel(vm: CoverViewModel, landscape: Boolean) {
    val draft by vm.draft.collectAsStateWithLifecycle()
    val d = draft ?: return
    val calendarPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) vm.updateDraft(d.copy(calendarEvents = false))
    }
    Column(
        (if (landscape) Modifier.width(EDITOR_WIDTH).fillMaxHeight() else Modifier.fillMaxWidth().height(420.dp))
            .background(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
                if (landscape) RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp) else RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            )
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(if (landscape) WindowInsetsSides.Vertical + WindowInsetsSides.End else WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
            )
    ) {
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Edit layar kunci", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            TextButton(onClick = vm::cancelEdit) { Text("Batal") }
            Button(onClick = vm::saveEdit) { Text("Simpan") }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            CoverStyleControls(
                cover = d,
                onChange = vm::updateDraft,
                onRequestCalendarPermission = { calendarPermission.launch(Manifest.permission.READ_CALENDAR) },
            )
            Box(Modifier.height(24.dp))
        }
    }
}
