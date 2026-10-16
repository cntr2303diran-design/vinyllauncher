package com.vinyl.launcher.data

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "vinyl_settings")

/**
 * Sumber kebenaran tunggal untuk semua pengaturan.
 * Disimpan sebagai JSON di DataStore Preferences.
 */
class SettingsRepository private constructor(private val context: Context) {

    private val key = stringPreferencesKey("settings_json")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Aliran mentah dari DataStore (tanpa nilai awal palsu). */
    val flow: Flow<AppSettings> = context.dataStore.data
        .map { prefs -> prefs[key]?.let { decode(it) } ?: AppSettings() }

    /** Pengaturan terkini; dibagikan ke semua layar. */
    val settings: StateFlow<AppSettings> = flow.stateIn(scope, SharingStarted.Eagerly, AppSettings())

    private fun decode(raw: String): AppSettings =
        runCatching { json.decodeFromString(AppSettings.serializer(), raw) }.getOrDefault(AppSettings())

    /** Ambil nilai yang benar-benar tersimpan (menunggu DataStore selesai dibaca). */
    suspend fun current(): AppSettings =
        context.dataStore.data.first()[key]?.let { decode(it) } ?: AppSettings()

    /** Ubah pengaturan secara atomik. */
    suspend fun update(transform: (AppSettings) -> AppSettings) {
        context.dataStore.edit { prefs ->
            val now = prefs[key]?.let { decode(it) } ?: AppSettings()
            prefs[key] = json.encodeToString(AppSettings.serializer(), transform(now))
        }
    }

    /** Reset gaya ke default, tetap mempertahankan status onboarding & aplikasi tersembunyi. */
    suspend fun resetToDefaults() = update {
        AppSettings(onboardingDone = it.onboardingDone, coverEnabled = it.coverEnabled)
    }

    /**
     * Menyalin gambar dari galeri ke penyimpanan internal agar tetap bisa dibaca
     * setelah izin URI sementara kedaluwarsa. Mengembalikan path file.
     */
    suspend fun importImage(uri: Uri, name: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(context.filesDir, "backgrounds").apply { mkdirs() }
            val out = File(dir, "$name-${System.currentTimeMillis()}.img")
            context.contentResolver.openInputStream(uri)?.use { input ->
                out.outputStream().use { input.copyTo(it) }
            }
            // Gambar lama sengaja tidak dihapus: bisa jadi masih dipakai preset tema.
            out.absolutePath
        }.getOrNull()
    }

    companion object {
        @Volatile private var instance: SettingsRepository? = null
        fun get(context: Context): SettingsRepository =
            instance ?: synchronized(this) {
                instance ?: SettingsRepository(context.applicationContext).also { instance = it }
            }
    }
}
