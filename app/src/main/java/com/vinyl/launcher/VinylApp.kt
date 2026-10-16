package com.vinyl.launcher

import android.app.Application
import com.vinyl.launcher.data.AppsRepository
import com.vinyl.launcher.data.SettingsRepository
import com.vinyl.launcher.media.MediaRepository
import com.vinyl.launcher.service.CoverService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Inisialisasi repository bersama dan sinkronisasi pengaturan → layanan. */
class VinylApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        MediaRepository.init(this)
        MediaRepository.connect()
        AppsRepository.get(this) // muat daftar aplikasi lebih awal

        val settings = SettingsRepository.get(this).settings
        appScope.launch {
            settings.map { it.defaultMusicPackage }.distinctUntilChanged()
                .collect { MediaRepository.defaultMusicPackage = it }
        }
        appScope.launch {
            // Nyalakan/matikan layanan layar kunci sesuai pengaturan
            SettingsRepository.get(this@VinylApp).flow.map { it.coverEnabled }.distinctUntilChanged().collect { enabled ->
                if (enabled) CoverService.start(this@VinylApp) else CoverService.stop(this@VinylApp)
            }
        }
    }
}
