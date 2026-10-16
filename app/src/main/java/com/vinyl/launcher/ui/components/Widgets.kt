package com.vinyl.launcher.ui.components

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Thunderstorm
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.vinyl.launcher.data.CalendarRepository
import com.vinyl.launcher.data.WeatherInfo
import com.vinyl.launcher.data.WeatherRepository
import com.vinyl.launcher.data.WeatherSettings
import kotlinx.coroutines.delay
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

// ======================= Kalender =======================

/**
 * Kalender mini satu bulan. Hari ini ditandai lingkaran kecil bergaya vinyl
 * (piringan hitam beralur dengan label berwarna [accent]).
 */
@Composable
fun MiniCalendar(
    month: YearMonth,
    today: LocalDate,
    eventDays: Set<LocalDate>,
    color: Color,
    accent: Color,
    modifier: Modifier = Modifier,
    cell: Dp = 30.dp,
) {
    val locale = Locale.getDefault()
    val firstDow = WeekFields.of(locale).firstDayOfWeek
    val days = remember(firstDow) { (0..6).map { firstDow.plus(it.toLong()) } }
    val leading = (month.atDay(1).dayOfWeek.value - firstDow.value + 7) % 7
    val total = month.lengthOfMonth()
    val fs = (cell.value * 0.42f).sp

    Column(modifier) {
        Text(
            month.month.getDisplayName(TextStyle.FULL_STANDALONE, locale).replaceFirstChar { it.titlecase(locale) } +
                " " + month.year,
            color = color,
            fontSize = (cell.value * 0.5f).sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 6.dp, start = 4.dp),
        )
        Row {
            days.forEach { d ->
                Text(
                    d.getDisplayName(TextStyle.NARROW_STANDALONE, locale),
                    color = color.copy(alpha = if (d == DayOfWeek.SUNDAY) 0.75f else 0.45f),
                    fontSize = fs * 0.85f,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(cell),
                )
            }
        }
        val rows = (leading + total + 6) / 7
        for (r in 0 until rows) {
            Row {
                for (c in 0 until 7) {
                    val dayNum = r * 7 + c - leading + 1
                    Box(Modifier.size(cell), contentAlignment = Alignment.Center) {
                        if (dayNum in 1..total) {
                            val date = month.atDay(dayNum)
                            val isToday = date == today
                            if (isToday) VinylDot(accent, Modifier.size(cell * 0.92f))
                            Text(
                                dayNum.toString(),
                                color = when {
                                    isToday -> Color.White
                                    date.isBefore(today) -> color.copy(alpha = 0.4f)
                                    else -> color.copy(alpha = 0.85f)
                                },
                                fontSize = fs,
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                            )
                            if (date in eventDays) {
                                Box(
                                    Modifier.align(Alignment.BottomCenter).padding(bottom = cell * 0.06f).size(4.dp)
                                ) {
                                    Canvas(Modifier.size(4.dp)) { drawCircle(if (isToday) Color.White else accent) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Lingkaran kecil bergaya vinyl untuk menandai hari ini. */
@Composable
fun VinylDot(accent: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val r = size.minDimension / 2f
        drawCircle(Color(0xFF0B0B0D), r)
        for (i in 1..3) drawCircle(Color.White.copy(alpha = 0.10f), r * (0.55f + i * 0.12f), style = Stroke(1f))
        drawCircle(accent.copy(alpha = 0.85f), r * 0.42f, style = Stroke(r * 0.12f))
    }
}

/** Dua kalender mini berdampingan: bulan ini & bulan depan (otomatis bergeser tiap awal bulan). */
@Composable
fun TwoMonthCalendar(
    today: LocalDate,
    showEvents: Boolean,
    color: Color,
    accent: Color,
    modifier: Modifier = Modifier,
    cell: Dp = 28.dp,
    vertical: Boolean = false,
) {
    val context = LocalContext.current
    val thisMonth = YearMonth.from(today)
    val nextMonth = thisMonth.plusMonths(1)
    val events by produceState(emptySet<LocalDate>(), thisMonth, showEvents) {
        value = if (showEvents) CalendarRepository.eventDays(context, thisMonth, nextMonth) else emptySet()
    }
    val content: @Composable () -> Unit = {
        MiniCalendar(thisMonth, today, events, color, accent, cell = cell)
        Spacer(Modifier.size(cell * 0.8f))
        MiniCalendar(nextMonth, today, events, color, accent, cell = cell)
    }
    if (vertical) Column(modifier) { content() } else Row(modifier) { content() }
}

// ======================= Baterai =======================

data class BatteryStatus(val level: Int, val charging: Boolean)

/** Status baterai via broadcast ACTION_BATTERY_CHANGED (sticky). */
@Composable
fun rememberBattery(): BatteryStatus {
    val context = LocalContext.current
    var status by remember { mutableStateOf(BatteryStatus(100, false)) }
    DisposableEffect(context) {
        fun parse(i: Intent?) {
            i ?: return
            val level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
            val plugged = i.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0
            if (level >= 0) status = BatteryStatus(level * 100 / scale.coerceAtLeast(1), plugged)
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) = parse(intent)
        }
        parse(
            ContextCompat.registerReceiver(
                context, receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED), ContextCompat.RECEIVER_NOT_EXPORTED,
            )
        )
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }
    return status
}

/** Indikator baterai minimalis (ikon digambar Canvas + persen). */
@Composable
fun BatteryIndicator(color: Color, modifier: Modifier = Modifier, scale: Float = 1f) {
    val b = rememberBattery()
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp * scale)) {
        Canvas(Modifier.size(width = 26.dp * scale, height = 13.dp * scale)) {
            val bodyW = size.width * 0.9f
            drawRoundRect(color.copy(alpha = 0.7f), size = Size(bodyW, size.height), cornerRadius = CornerRadius(size.height * 0.25f), style = Stroke(size.height * 0.1f))
            drawRoundRect(color.copy(alpha = 0.7f), topLeft = Offset(bodyW + 1f, size.height * 0.3f), size = Size(size.width * 0.08f, size.height * 0.4f))
            val fillColor = when {
                b.charging -> Color(0xFF7BD389)
                b.level <= 15 -> Color(0xFFE5534B)
                else -> color
            }
            val inset = size.height * 0.2f
            drawRoundRect(
                fillColor,
                topLeft = Offset(inset, inset),
                size = Size((bodyW - inset * 2) * b.level / 100f, size.height - inset * 2),
                cornerRadius = CornerRadius(size.height * 0.1f),
            )
        }
        Text("${b.level}%" + if (b.charging) " ⚡" else "", color = color, fontSize = (14 * scale).sp)
    }
}

// ======================= Cuaca =======================

@Composable
fun WeatherWidget(settings: WeatherSettings, color: Color, modifier: Modifier = Modifier, scale: Float = 1f) {
    val weather by produceState<WeatherInfo?>(null, settings) {
        while (true) {
            value = WeatherRepository.current(settings) ?: value
            delay(30 * 60 * 1000L)
        }
    }
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp * scale)) {
        val icon = when (weather?.code ?: -1) {
            -1 -> Icons.Rounded.Cloud
            0, 1 -> Icons.Rounded.WbSunny
            in 51..67, in 80..82 -> Icons.Rounded.WaterDrop
            in 95..99 -> Icons.Rounded.Thunderstorm
            else -> Icons.Rounded.Cloud
        }
        Icon(icon, null, tint = color, modifier = Modifier.size(30.dp * scale))
        Column {
            Text(
                weather?.let { "${it.temperatureC.toInt()}°" } ?: "--°",
                color = color, fontSize = (26 * scale).sp, fontWeight = FontWeight.Light,
            )
            Text(
                weather?.let { "${it.city} · ${it.description}" } ?: settings.cityName,
                color = color.copy(alpha = 0.7f), fontSize = (12 * scale).sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// ======================= Info lagu =======================

@Composable
fun SongInfo(title: String, artist: String, color: Color, modifier: Modifier = Modifier, alignEnd: Boolean = false) {
    Column(modifier, horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start) {
        Text(
            title.ifBlank { "Tidak ada yang diputar" },
            color = color, fontSize = 20.sp, fontWeight = FontWeight.Medium,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        if (artist.isNotBlank()) {
            Spacer(Modifier.height(2.dp))
            Text(artist, color = color.copy(alpha = 0.7f), fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
