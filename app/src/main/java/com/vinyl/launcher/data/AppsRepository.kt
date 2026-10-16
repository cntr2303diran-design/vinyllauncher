package com.vinyl.launcher.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.graphics.Rect
import android.net.Uri
import android.os.UserHandle
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.Collator
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/** Satu aplikasi yang bisa diluncurkan. [key] = komponen yang di-flatten (stabil untuk disimpan). */
data class AppInfo(
    val key: String,
    val packageName: String,
    val label: String,
    val icon: ImageBitmap,
    val component: ComponentName,
    val user: UserHandle,
)

/**
 * Daftar aplikasi via LauncherApps (mendukung profil kerja),
 * diperbarui otomatis saat aplikasi dipasang/dihapus/diperbarui.
 */
class AppsRepository private constructor(private val context: Context) {

    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val iconCache = ConcurrentHashMap<String, ImageBitmap>()
    private val iconSizePx = (context.resources.displayMetrics.density * 56).toInt()

    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    /** Semua aplikasi, urut abjad (sadar locale). */
    val apps: StateFlow<List<AppInfo>> = _apps.asStateFlow()

    private val callback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String, user: UserHandle) = invalidate(packageName)
        override fun onPackageAdded(packageName: String, user: UserHandle) = invalidate(packageName)
        override fun onPackageChanged(packageName: String, user: UserHandle) = invalidate(packageName)
        override fun onPackagesAvailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) = refresh()
        override fun onPackagesUnavailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) = refresh()
    }

    init {
        launcherApps.registerCallback(callback)
        refresh()
    }

    private fun invalidate(pkg: String) {
        iconCache.keys.filter { it.startsWith("$pkg/") }.forEach { iconCache.remove(it) }
        refresh()
    }

    fun refresh() {
        scope.launch {
            val collator = Collator.getInstance(Locale.getDefault())
            val list = launcherApps.profiles.flatMap { user ->
                launcherApps.getActivityList(null, user)
            }
                .filter { it.componentName.packageName != context.packageName || it.componentName.className.endsWith("OnboardingActivity") }
                .map { it.toAppInfo() }
                .sortedWith { a, b -> collator.compare(a.label, b.label) }
            _apps.value = list
        }
    }

    private fun LauncherActivityInfo.toAppInfo(): AppInfo {
        val key = componentName.flattenToString() + if (user != android.os.Process.myUserHandle()) "#${user.hashCode()}" else ""
        val icon = iconCache.getOrPut(key) {
            getBadgedIcon(0).toBitmap(iconSizePx, iconSizePx).asImageBitmap()
        }
        return AppInfo(key, componentName.packageName, label.toString(), icon, componentName, user)
    }

    fun find(key: String): AppInfo? = _apps.value.firstOrNull { it.key == key }
    fun findByPackage(pkg: String): AppInfo? = _apps.value.firstOrNull { it.packageName == pkg }

    fun launch(app: AppInfo, bounds: Rect? = null) {
        runCatching { launcherApps.startMainActivity(app.component, app.user, bounds, null) }
    }

    /** Buka sebagai jendela di tengah layar (±62% × 70%); jika sistem tidak mendukung, terbuka biasa. */
    fun open(app: AppInfo, windowed: Boolean) {
        if (!windowed) { launch(app); return }
        val dm = context.resources.displayMetrics
        val w = (dm.widthPixels * 0.62f).toInt()
        val h = (dm.heightPixels * 0.7f).toInt()
        val l = (dm.widthPixels - w) / 2
        val t = (dm.heightPixels - h) / 2
        val opts = android.app.ActivityOptions.makeBasic().setLaunchBounds(Rect(l, t, l + w, t + h)).toBundle()
        runCatching { launcherApps.startMainActivity(app.component, app.user, null, opts) }
            .onFailure { launch(app) }
    }

    fun launchPackage(pkg: String) {
        findByPackage(pkg)?.let { launch(it) } ?: run {
            context.packageManager.getLaunchIntentForPackage(pkg)?.let {
                it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                runCatching { context.startActivity(it) }
            }
        }
    }

    fun openAppInfo(app: AppInfo) {
        runCatching { launcherApps.startAppDetailsActivity(app.component, app.user, null, null) }
    }

    fun uninstall(app: AppInfo) {
        val intent = Intent(Intent.ACTION_DELETE, Uri.parse("package:${app.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    companion object {
        @Volatile private var instance: AppsRepository? = null
        fun get(context: Context): AppsRepository =
            instance ?: synchronized(this) {
                instance ?: AppsRepository(context.applicationContext).also { instance = it }
            }
    }
}
