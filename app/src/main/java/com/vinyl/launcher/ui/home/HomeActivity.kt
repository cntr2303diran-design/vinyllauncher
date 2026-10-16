package com.vinyl.launcher.ui.home

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.vinyl.launcher.data.SettingsRepository
import com.vinyl.launcher.media.MediaRepository
import com.vinyl.launcher.ui.onboarding.OnboardingActivity
import com.vinyl.launcher.ui.theme.VinylTheme
import kotlinx.coroutines.launch

/**
 * Activity HOME. Perubahan orientasi/ukuran ditangani Compose (configChanges di manifest)
 * sehingga animasi vinyl tidak terputus saat tablet diputar.
 */
class HomeActivity : ComponentActivity() {

    private val vm: HomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent { VinylTheme { HomeScreen(vm) } }

        // Pertama kali: arahkan ke onboarding
        lifecycleScope.launch {
            if (!SettingsRepository.get(this@HomeActivity).current().onboardingDone) {
                startActivity(Intent(this@HomeActivity, OnboardingActivity::class.java))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Izin akses notifikasi mungkin baru saja diberikan
        MediaRepository.connect()
    }

    /** Tombol Home ditekan saat launcher sudah tampil → tutup drawer/editor. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)) vm.onHomePressed()
    }
}
