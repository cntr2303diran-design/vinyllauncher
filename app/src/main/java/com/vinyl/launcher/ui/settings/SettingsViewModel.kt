package com.vinyl.launcher.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vinyl.launcher.data.AppInfo
import com.vinyl.launcher.data.AppSettings
import com.vinyl.launcher.data.AppsRepository
import com.vinyl.launcher.data.SettingsRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** ViewModel halaman Pengaturan: semua perubahan langsung disimpan ke DataStore. */
class SettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = SettingsRepository.get(app)
    val settings: StateFlow<AppSettings> = repo.settings
    val apps: StateFlow<List<AppInfo>> = AppsRepository.get(app).apps

    fun update(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch { repo.update(transform) }
    }

    fun reset() {
        viewModelScope.launch { repo.resetToDefaults() }
    }
}
