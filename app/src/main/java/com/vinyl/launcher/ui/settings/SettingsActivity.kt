package com.vinyl.launcher.ui.settings

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vinyl.launcher.data.MAX_DOCK_APPS
import com.vinyl.launcher.ui.cover.CoverActivity
import com.vinyl.launcher.ui.onboarding.PermissionSteps
import com.vinyl.launcher.ui.theme.VinylTheme

/** Halaman Pengaturan lengkap. Semua opsi disimpan langsung (DataStore). */
class SettingsActivity : ComponentActivity() {
    private val vm: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { VinylTheme { SettingsScreen(vm, onBack = ::finish) } }
    }
}

@Composable
private fun SettingsScreen(vm: SettingsViewModel, onBack: () -> Unit) {
    val s by vm.settings.collectAsStateWithLifecycle()
    val apps by vm.apps.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var confirmReset by remember { mutableStateOf(false) }
    var pickMusic by remember { mutableStateOf(false) }
    var pickDock by remember { mutableIntStateOf(-2) } // -2 = tidak aktif, -1 = tambah, >=0 = ganti index
    val calendarPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) vm.update { it.copy(cover = it.cover.copy(calendarEvents = false)) }
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).windowInsetsPadding(WindowInsets.safeDrawing)) {
        Column(
            Modifier.align(Alignment.TopCenter).widthIn(max = 760.dp).fillMaxWidth()
                .verticalScroll(rememberScrollState()).padding(horizontal = 28.dp, vertical = 20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Pengaturan", fontSize = 32.sp, modifier = Modifier.weight(1f))
                TextButton(onClick = onBack) { Text("Tutup") }
            }

            SectionTitle("Izin & status")
            PermissionSteps(s.coverEnabled) { on -> vm.update { it.copy(coverEnabled = on) } }

            // ---------- Home ----------
            Group("Home screen") {
                HomeStyleControls(s.home) { h -> vm.update { it.copy(home = h) } }
            }

            // ---------- Layar kunci ----------
            Group("Layar kunci vinyl") {
                SwitchRow(
                    "Aktifkan layar kunci vinyl", s.coverEnabled, { on -> vm.update { it.copy(coverEnabled = on) } },
                    subtitle = "Muncul di atas layar kunci sistem setiap layar dinyalakan",
                )
                OutlinedButton(onClick = { CoverActivity.show(context) }, modifier = Modifier.padding(top = 4.dp)) { Text("Pratinjau layar kunci") }
                CoverStyleControls(
                    s.cover,
                    onChange = { c -> vm.update { it.copy(cover = c) } },
                    onRequestCalendarPermission = { calendarPermission.launch(Manifest.permission.READ_CALENDAR) },
                )
            }

            // ---------- Musik ----------
            Group("Musik") {
                val current = apps.firstOrNull { it.packageName == s.defaultMusicPackage }
                Row(
                    Modifier.fillMaxWidth().clickable { pickMusic = true }.padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Aplikasi musik default", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            current?.label ?: "Otomatis (aplikasi yang sedang memutar)",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    current?.let { Image(it.icon, null, Modifier.size(36.dp)) }
                }
                if (s.defaultMusicPackage != null) TextButton(onClick = { vm.update { it.copy(defaultMusicPackage = null) } }) { Text("Kembali ke otomatis") }
            }

            // ---------- Dock ----------
            Group("Dock") {
                SwitchRow(
                    "Dock vinyl mini", s.dock.enabled, { on -> vm.update { it.copy(dock = it.dock.copy(enabled = on)) } },
                    subtitle = "Cadangan bila taskbar sistem hilang saat memakai launcher pihak ketiga",
                )
                if (s.dock.enabled) {
                    s.dock.apps.forEachIndexed { i, key ->
                        val app = apps.firstOrNull { it.key == key }
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            app?.let { Image(it.icon, null, Modifier.size(32.dp)) }
                            Spacer(Modifier.width(12.dp))
                            Text(app?.label ?: key, Modifier.weight(1f))
                            TextButton(onClick = { pickDock = i }) { Text("Ganti") }
                            TextButton(onClick = { vm.update { st -> st.copy(dock = st.dock.copy(apps = st.dock.apps.filterIndexed { j, _ -> j != i })) } }) { Text("Hapus") }
                        }
                    }
                    if (s.dock.apps.size < MAX_DOCK_APPS) OutlinedButton(onClick = { pickDock = -1 }) { Text("+ Tambah aplikasi (${s.dock.apps.size}/$MAX_DOCK_APPS)") }
                }
            }

            // ---------- Gestur ----------
            Group("Gestur home screen") {
                GestureControls(s.gestures, apps) { g -> vm.update { it.copy(gestures = g) } }
            }

            // ---------- Cuaca ----------
            Group("Cuaca") {
                var city by remember(s.weather) { mutableStateOf(s.weather.cityName) }
                var lat by remember(s.weather) { mutableStateOf(s.weather.latitude.toString()) }
                var lon by remember(s.weather) { mutableStateOf(s.weather.longitude.toString()) }
                OutlinedTextField(city, { city = it }, label = { Text("Nama kota") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 8.dp)) {
                    OutlinedTextField(lat, { lat = it }, label = { Text("Lintang") }, singleLine = true, modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    OutlinedTextField(lon, { lon = it }, label = { Text("Bujur") }, singleLine = true, modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                }
                Button(onClick = {
                    val la = lat.toDoubleOrNull(); val lo = lon.toDoubleOrNull()
                    if (la != null && lo != null) vm.update { it.copy(weather = it.weather.copy(cityName = city, latitude = la, longitude = lo)) }
                }, modifier = Modifier.padding(top = 8.dp)) { Text("Simpan lokasi cuaca") }
            }

            // ---------- Aplikasi tersembunyi ----------
            Group("Aplikasi tersembunyi") {
                if (s.hiddenApps.isEmpty()) Text("Tidak ada. Sembunyikan aplikasi dengan menekan lama ikonnya di app drawer.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                s.hiddenApps.forEach { key ->
                    val app = apps.firstOrNull { it.key == key }
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        app?.let { Image(it.icon, null, Modifier.size(32.dp)) }
                        Spacer(Modifier.width(12.dp))
                        Text(app?.label ?: key, Modifier.weight(1f))
                        TextButton(onClick = { vm.update { it.copy(hiddenApps = it.hiddenApps - key) } }) { Text("Tampilkan") }
                    }
                }
            }

            // ---------- Preset ----------
            Group("Preset tema") {
                if (s.presets.isEmpty()) Text("Belum ada preset. Simpan gaya dari mode edit home screen (tekan lama area kosong).", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                s.presets.forEach { p ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(p.name, Modifier.weight(1f))
                        TextButton(onClick = { vm.update { it.copy(home = p.home, cover = p.cover) } }) { Text("Terapkan") }
                        TextButton(onClick = { vm.update { st -> st.copy(presets = st.presets.filterNot { it.id == p.id }) } }) { Text("Hapus") }
                    }
                }
            }

            Spacer(Modifier.height(28.dp))
            Button(
                onClick = { confirmReset = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Reset ke default") }
            Spacer(Modifier.height(40.dp))
        }
    }

    if (confirmReset) AlertDialog(
        onDismissRequest = { confirmReset = false },
        title = { Text("Reset semua pengaturan?") },
        text = { Text("Gaya home, layar kunci, gestur, dock, preset, dan aplikasi tersembunyi akan dikembalikan ke default.") },
        confirmButton = { TextButton(onClick = { vm.reset(); confirmReset = false }) { Text("Reset") } },
        dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Batal") } },
    )
    if (pickMusic) AppPickerDialog(apps, "Aplikasi musik default", onDismiss = { pickMusic = false }) { app ->
        vm.update { it.copy(defaultMusicPackage = app.packageName) }; pickMusic = false
    }
    if (pickDock != -2) AppPickerDialog(apps, "Pilih aplikasi dock", onDismiss = { pickDock = -2 }) { app ->
        val idx = pickDock
        vm.update { st ->
            val list = st.dock.apps.toMutableList()
            if (idx >= 0 && idx < list.size) list[idx] = app.key else if (app.key !in list) list += app.key
            st.copy(dock = st.dock.copy(apps = list.distinct().take(MAX_DOCK_APPS)))
        }
        pickDock = -2
    }
}

/** Kelompok pengaturan dengan judul & pemisah. */
@Composable
private fun Group(title: String, content: @Composable () -> Unit) {
    Spacer(Modifier.height(12.dp))
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 20.dp, bottom = 4.dp))
    Column(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp)).padding(horizontal = 18.dp, vertical = 8.dp)
    ) { content() }
}
