package com.vinyl.launcher.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vinyl.launcher.data.AppInfo
import com.vinyl.launcher.data.AppSettings
import com.vinyl.launcher.data.AppsRepository
import com.vinyl.launcher.data.HomeSettings
import com.vinyl.launcher.data.MAX_DOCK_APPS
import com.vinyl.launcher.data.SettingsRepository
import com.vinyl.launcher.data.ThemePreset
import com.vinyl.launcher.media.MediaRepository
import com.vinyl.launcher.media.NowPlaying
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel home screen.
 * Mode edit memakai "draft": perubahan langsung terlihat (pratinjau) tapi baru
 * disimpan ke DataStore saat pengguna menekan Simpan.
 */
class HomeViewModel(app: Application) : AndroidViewModel(app) {

    private val settingsRepo = SettingsRepository.get(app)
    private val appsRepo = AppsRepository.get(app)

    val settings: StateFlow<AppSettings> = settingsRepo.settings
    val nowPlaying: StateFlow<NowPlaying> = MediaRepository.state

    /** Aplikasi yang terlihat di drawer (tanpa yang disembunyikan). */
    val visibleApps: StateFlow<List<AppInfo>> = combine(appsRepo.apps, settings) { apps, s ->
        apps.filter { it.key !in s.hiddenApps }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allApps: StateFlow<List<AppInfo>> = appsRepo.apps

    /** Aplikasi di dock vinyl, sesuai urutan yang disimpan. */
    val dockApps: StateFlow<List<AppInfo>> = combine(appsRepo.apps, settings) { apps, s ->
        s.dock.apps.mapNotNull { key -> apps.firstOrNull { it.key == key } }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _drawerOpen = MutableStateFlow(false)
    val drawerOpen: StateFlow<Boolean> = _drawerOpen.asStateFlow()

    private val _draft = MutableStateFlow<HomeSettings?>(null)
    /** Non-null saat mode edit aktif. */
    val draft: StateFlow<HomeSettings?> = _draft.asStateFlow()

    /** Gaya yang sedang ditampilkan (draft saat edit, tersimpan saat normal). */
    val home: StateFlow<HomeSettings> = combine(settings, _draft) { s, d -> d ?: s.home }
        .stateIn(viewModelScope, SharingStarted.Eagerly, HomeSettings())

    // ---------- Drawer ----------
    fun openDrawer() { if (_draft.value == null) _drawerOpen.value = true }
    fun closeDrawer() { _drawerOpen.value = false }

    /** Tombol Home ditekan saat launcher sudah di depan: tutup drawer/editor. */
    fun onHomePressed() {
        _drawerOpen.value = false
        if (_draft.value != null) cancelEdit()
    }

    // ---------- Mode edit ----------
    fun startEdit() {
        _drawerOpen.value = false
        _draft.value = settings.value.home
    }
    fun updateDraft(transform: (HomeSettings) -> HomeSettings) = _draft.update { it?.let(transform) }
    fun cancelEdit() { _draft.value = null }
    fun saveEdit() {
        val d = _draft.value ?: return
        viewModelScope.launch { settingsRepo.update { it.copy(home = d) } }
        _draft.value = null
    }

    /** Pengaturan non-gaya (mis. gestur) disimpan langsung tanpa draft. */
    fun updateSettings(transform: (AppSettings) -> AppSettings) = viewModelScope.launch { settingsRepo.update(transform) }

    // ---------- Preset tema ----------
    fun savePreset(name: String) = viewModelScope.launch {
        val home = _draft.value ?: settings.value.home
        settingsRepo.update {
            it.copy(presets = it.presets + ThemePreset(System.currentTimeMillis(), name.ifBlank { "Preset ${it.presets.size + 1}" }, home, it.cover))
        }
    }
    fun applyPreset(preset: ThemePreset) {
        if (_draft.value != null) _draft.value = preset.home
        else viewModelScope.launch { settingsRepo.update { it.copy(home = preset.home, cover = preset.cover) } }
    }
    fun deletePreset(id: Long) = viewModelScope.launch {
        settingsRepo.update { s -> s.copy(presets = s.presets.filterNot { it.id == id }) }
    }

    // ---------- Aplikasi ----------
    fun launch(app: AppInfo) { appsRepo.launch(app); closeDrawer() }
    fun appInfo(app: AppInfo) = appsRepo.openAppInfo(app)
    fun uninstall(app: AppInfo) = appsRepo.uninstall(app)
    fun hideApp(app: AppInfo) = viewModelScope.launch {
        settingsRepo.update { it.copy(hiddenApps = it.hiddenApps + app.key) }
    }

    fun addToDock(app: AppInfo) = viewModelScope.launch {
        settingsRepo.update { s ->
            if (app.key in s.dock.apps || s.dock.apps.size >= MAX_DOCK_APPS) s
            else s.copy(dock = s.dock.copy(apps = s.dock.apps + app.key))
        }
    }
    fun replaceDock(index: Int, app: AppInfo) = viewModelScope.launch {
        settingsRepo.update { s ->
            val list = s.dock.apps.toMutableList()
            if (index in list.indices) list[index] = app.key
            s.copy(dock = s.dock.copy(apps = list.distinct()))
        }
    }
    fun removeDock(index: Int) = viewModelScope.launch {
        settingsRepo.update { s ->
            s.copy(dock = s.dock.copy(apps = s.dock.apps.filterIndexed { i, _ -> i != index }))
        }
    }

    // ---------- Musik ----------
    fun togglePlay() = MediaRepository.togglePlayPause()
    fun next() = MediaRepository.next()
    fun previous() = MediaRepository.previous()
    fun seek(ms: Long) = MediaRepository.seekTo(ms)
    fun openMusic() = MediaRepository.openMusicApp(getApplication())
}
