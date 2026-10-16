package com.vinyl.launcher.service

import android.app.AppOpsManager
import android.app.KeyguardManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.os.Process
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.vinyl.launcher.R
import com.vinyl.launcher.data.AppInfo
import com.vinyl.launcher.data.AppsRepository
import com.vinyl.launcher.data.SettingsRepository
import com.vinyl.launcher.ui.settings.SettingsActivity
import com.vinyl.launcher.ui.taskbar.TaskbarBar
import com.vinyl.launcher.ui.theme.VinylTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Taskbar Vinyl: jendela overlay (TYPE_APPLICATION_OVERLAY) di bagian bawah layar yang tampil
 * di atas semua aplikasi — pengganti taskbar Samsung yang dimatikan sistem saat memakai
 * launcher pihak ketiga. Membutuhkan izin "Tampilkan di atas aplikasi lain".
 */
class TaskbarService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var wm: WindowManager
    private var view: ComposeView? = null
    private val owner = OverlayOwner()

    /** Aplikasi terakhir dibuka (dari UsageStats). */
    private val recents = MutableStateFlow<List<AppInfo>>(emptyList())
    private val screenUnlocked = MutableStateFlow(true)
    private val systemBarsVisible = MutableStateFlow(true)

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> screenUnlocked.value = false
                Intent.ACTION_USER_PRESENT -> screenUnlocked.value = true
                Intent.ACTION_SCREEN_ON -> screenUnlocked.value = !isLocked()
            }
        }
    }

    private fun isLocked() = getSystemService(KeyguardManager::class.java)?.isKeyguardLocked == true

    override fun onCreate() {
        super.onCreate()
        startAsForeground()
        if (!Settings.canDrawOverlays(this)) { stopSelf(); return }
        wm = getSystemService(WindowManager::class.java)
        screenUnlocked.value = !isLocked()
        ContextCompat.registerReceiver(
            this, screenReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF); addAction(Intent.ACTION_SCREEN_ON); addAction(Intent.ACTION_USER_PRESENT)
            },
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        owner.start()
        addOverlay()

        // Perbarui daftar aplikasi terakhir secara berkala selama taskbar tampil
        scope.launch {
            while (isActive) {
                if (screenUnlocked.value) refreshRecents()
                delay(4000)
            }
        }
        // Tampilkan/sembunyikan sesuai: layar terkunci, layar kunci vinyl, layar penuh
        scope.launch {
            combine(
                screenUnlocked, systemBarsVisible, TaskbarState.suppressed,
                SettingsRepository.get(this@TaskbarService).settings,
            ) { unlocked, barsVisible, suppressed, s ->
                unlocked && !suppressed && (barsVisible || !s.dock.hideInFullscreen)
            }.collect { show -> view?.visibility = if (show) View.VISIBLE else View.GONE }
        }
    }

    private fun addOverlay() {
        val v = ComposeView(this).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setViewTreeViewModelStoreOwner(owner)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                VinylTheme {
                    TaskbarBar(recents = recents)
                }
            }
        }
        // Deteksi aplikasi layar penuh: jika bilah sistem disembunyikan, taskbar ikut sembunyi
        ViewCompat.setOnApplyWindowInsetsListener(v) { _, insets ->
            systemBarsVisible.value = insets.isVisible(WindowInsetsCompat.Type.navigationBars()) ||
                insets.isVisible(WindowInsetsCompat.Type.statusBars())
            insets
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = (resources.displayMetrics.density * 6).toInt()
            windowAnimations = android.R.style.Animation_Toast
        }
        runCatching { wm.addView(v, params) }.onSuccess { view = v }
    }

    /** Aplikasi yang terakhir dibuka (tanpa launcher & aplikasi ini), terbaru dulu. */
    private suspend fun refreshRecents() {
        if (!hasUsageAccess(this)) { recents.value = emptyList(); return }
        val list = withContext(Dispatchers.IO) {
            runCatching {
                val usm = getSystemService(UsageStatsManager::class.java)
                val now = System.currentTimeMillis()
                val events = usm.queryEvents(now - 6 * 60 * 60 * 1000L, now)
                val e = UsageEvents.Event()
                val lastUsed = LinkedHashMap<String, Long>()
                while (events.hasNextEvent()) {
                    events.getNextEvent(e)
                    if (e.eventType == UsageEvents.Event.ACTIVITY_RESUMED) lastUsed[e.packageName] = e.timeStamp
                }
                val home = packageManager.resolveActivity(
                    Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0,
                )?.activityInfo?.packageName
                val apps = AppsRepository.get(this@TaskbarService)
                lastUsed.entries
                    .sortedByDescending { it.value }
                    .map { it.key }
                    .filter { it != packageName && it != home && it != "com.android.systemui" }
                    .mapNotNull { apps.findByPackage(it) }
                    .take(4)
            }.getOrDefault(emptyList())
        }
        recents.value = list
    }

    private fun startAsForeground() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "Taskbar Vinyl", NotificationManager.IMPORTANCE_MIN).apply { setShowBadge(false) }
        )
        val pi = PendingIntent.getActivity(
            this, 1, Intent(this, SettingsActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notif = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Taskbar Vinyl aktif")
            .setContentIntent(pi)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        ServiceCompat.startForeground(this, NOTIF_ID, notif, type)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        scope.cancel()
        runCatching { unregisterReceiver(screenReceiver) }
        view?.let { runCatching { wm.removeView(it) } }
        view = null
        owner.stop()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL = "taskbar"
        private const val NOTIF_ID = 45

        fun start(context: Context) {
            if (!Settings.canDrawOverlays(context)) return
            runCatching { ContextCompat.startForegroundService(context, Intent(context, TaskbarService::class.java)) }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, TaskbarService::class.java))
        }

        /** Apakah izin "Akses penggunaan" sudah diberikan (untuk daftar aplikasi terakhir). */
        fun hasUsageAccess(context: Context): Boolean {
            val ops = context.getSystemService(AppOpsManager::class.java) ?: return false
            val mode = ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
            return mode == AppOpsManager.MODE_ALLOWED
        }
    }
}

/** Status global yang memengaruhi taskbar (mis. disembunyikan saat layar kunci vinyl tampil). */
object TaskbarState {
    private val _suppressed = MutableStateFlow(false)
    val suppressed: StateFlow<Boolean> = _suppressed.asStateFlow()
    fun setSuppressed(value: Boolean) { _suppressed.value = value }
}

/** Pemilik lifecycle untuk ComposeView yang hidup di luar Activity (jendela overlay). */
private class OverlayOwner : LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner {
    private val registry = LifecycleRegistry(this)
    private val savedState = SavedStateRegistryController.create(this)
    override val lifecycle: Lifecycle get() = registry
    override val savedStateRegistry: SavedStateRegistry get() = savedState.savedStateRegistry
    override val viewModelStore = ViewModelStore()

    fun start() {
        savedState.performRestore(null)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    fun stop() {
        if (registry.currentState == Lifecycle.State.INITIALIZED) return
        registry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        viewModelStore.clear()
    }
}
