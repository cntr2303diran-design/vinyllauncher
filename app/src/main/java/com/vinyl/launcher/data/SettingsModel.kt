package com.vinyl.launcher.data

import kotlinx.serialization.Serializable

/*
 * Model pengaturan Vinyl Launcher.
 * Seluruh pengaturan disimpan sebagai satu dokumen JSON di DataStore
 * (lihat SettingsRepository) sehingga mudah di-reset, disalin ke preset,
 * dan ditambah field baru tanpa migrasi (field baru memakai nilai default).
 * Warna disimpan sebagai Long ARGB (0xAARRGGBB).
 */

/** Posisi 3x3 di dalam sebuah area. */
@Serializable
enum class Anchor { TOP_START, TOP_CENTER, TOP_END, CENTER_START, CENTER, CENTER_END, BOTTOM_START, BOTTOM_CENTER, BOTTOM_END }

@Serializable
enum class ClockType { THIN, BOLD, FLIP, ANALOG, CIRCULAR }

@Serializable
enum class FontChoice { DEFAULT, SANS, SERIF, MONO, CURSIVE }

@Serializable
enum class BackgroundType { ALBUM, SOLID, GRADIENT, IMAGE }

@Serializable
enum class TurntablePosition { LEFT, CENTER, RIGHT }

@Serializable
enum class VinylStyle { CLASSIC, COLORED, TRANSPARENT, PICTURE_DISC }

@Serializable
enum class TonearmStyle { SILVER, BLACK, GOLD }

@Serializable
enum class GestureAction { NONE, APP_DRAWER, NOTIFICATIONS, LOCK_SCREEN, OPEN_APP, SETTINGS }

@Serializable
enum class WidgetType { CALENDAR, WEATHER, BATTERY }

/** Gaya jam yang dipakai di home screen maupun layar kunci. */
@Serializable
data class ClockSettings(
    val visible: Boolean = true,
    val type: ClockType = ClockType.THIN,
    val font: FontChoice = FontChoice.DEFAULT,
    val color: Long = 0xFFF2EFEA,
    /** Skala ukuran relatif (0.5 – 2.0). */
    val scale: Float = 1f,
    val anchor: Anchor = Anchor.TOP_START,
    val use24h: Boolean = true,
    val showDate: Boolean = true,
)

@Serializable
data class BackgroundSettings(
    val type: BackgroundType = BackgroundType.ALBUM,
    val solidColor: Long = 0xFF1B1B1F,
    val gradientStart: Long = 0xFF2B1E3A,
    val gradientEnd: Long = 0xFF0E0E10,
    /** Path file internal hasil salinan gambar dari galeri. */
    val imagePath: String? = null,
)

/** Widget tambahan di home screen. Posisi dalam fraksi layar (0..1) agar tahan rotasi. */
@Serializable
data class WidgetConfig(
    val id: Long,
    val type: WidgetType,
    val x: Float = 0.75f,
    val y: Float = 0.1f,
    val scale: Float = 1f,
)

@Serializable
data class HomeSettings(
    val background: BackgroundSettings = BackgroundSettings(),
    /** Ukuran turntable relatif terhadap ruang yang tersedia (0.5 – 1.0). */
    val turntableSize: Float = 0.85f,
    val turntablePosition: TurntablePosition = TurntablePosition.CENTER,
    val vinylStyle: VinylStyle = VinylStyle.CLASSIC,
    val tonearmStyle: TonearmStyle = TonearmStyle.SILVER,
    val coverVisible: Boolean = true,
    val coverRounded: Boolean = true,
    val coverShadow: Boolean = true,
    val rpm: Int = 33,
    val clock: ClockSettings = ClockSettings(scale = 0.45f, anchor = Anchor.TOP_START),
    val widgets: List<WidgetConfig> = emptyList(),
)

/** Elemen yang bisa diatur di layar kunci. */
@Serializable
data class CoverElement(val visible: Boolean = true, val anchor: Anchor = Anchor.CENTER)

@Serializable
data class CoverSettings(
    val clock: ClockSettings = ClockSettings(type = ClockType.THIN, scale = 1.4f, anchor = Anchor.CENTER),
    val date: CoverElement = CoverElement(true, Anchor.CENTER),
    val calendar: CoverElement = CoverElement(true, Anchor.CENTER),
    val songInfo: CoverElement = CoverElement(true, Anchor.TOP_START),
    val battery: CoverElement = CoverElement(true, Anchor.TOP_END),
    val calendarEvents: Boolean = false,
    val background: BackgroundSettings = BackgroundSettings(type = BackgroundType.ALBUM),
)

@Serializable
data class GestureBinding(val action: GestureAction = GestureAction.NONE, val packageName: String? = null)

@Serializable
data class GestureSettings(
    val swipeUp: GestureBinding = GestureBinding(GestureAction.APP_DRAWER),
    val swipeDown: GestureBinding = GestureBinding(GestureAction.NOTIFICATIONS),
    val doubleTap: GestureBinding = GestureBinding(GestureAction.LOCK_SCREEN),
)

@Serializable
data class DockSettings(
    val enabled: Boolean = false,
    /** Komponen aplikasi ("paket/kelas"), maks 8. */
    val apps: List<String> = emptyList(),
)

@Serializable
data class WeatherSettings(
    val cityName: String = "Jakarta",
    val latitude: Double = -6.2088,
    val longitude: Double = 106.8456,
)

/** Preset tema: salinan gaya home + layar kunci. */
@Serializable
data class ThemePreset(val id: Long, val name: String, val home: HomeSettings, val cover: CoverSettings)

@Serializable
data class AppSettings(
    val home: HomeSettings = HomeSettings(),
    val cover: CoverSettings = CoverSettings(),
    val gestures: GestureSettings = GestureSettings(),
    val dock: DockSettings = DockSettings(),
    val weather: WeatherSettings = WeatherSettings(),
    /** Paket aplikasi musik default (null = pilih otomatis). */
    val defaultMusicPackage: String? = null,
    val hiddenApps: Set<String> = emptySet(),
    val presets: List<ThemePreset> = emptyList(),
    val coverEnabled: Boolean = false,
    val onboardingDone: Boolean = false,
)

const val MAX_DOCK_APPS = 8
