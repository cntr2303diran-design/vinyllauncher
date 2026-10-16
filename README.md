# Vinyl Launcher

Launcher Android untuk tablet dengan konsep **"tablet sebagai turntable"** — Kotlin + Jetpack Compose, minSdk 29, targetSdk 36. Dioptimalkan untuk landscape; portrait tetap didukung dengan tata letak yang menyesuaikan.

## Fitur

| Bagian | Isi |
|---|---|
| Home screen | Latar warna dominan cover (Palette API) dengan transisi halus, turntable Canvas (sampul, vinyl beralur & berkilap, tonearm), jam kecil di kiri atas, tanpa ikon aplikasi |
| Animasi | Play: vinyl meluncur keluar → tonearm turun → berputar 33/45 RPM. Pause: tonearm naik → melambat → masuk sebagian ke sampul |
| Interaksi | Ketuk sampul = play/pause · geser sampul = next/previous · seret tonearm = seek · putar vinyl dengan jari = maju/mundur |
| Layar kunci vinyl | Tampil di atas keyguard sistem, vinyl berlabel cover, jam & tanggal besar, kalender dua bulan (opsional titik acara), geser kanan → PIN sistem, tekan lama → verifikasi PIN → editor |
| Kustomisasi | Tekan lama area kosong: latar (cover/solid/gradasi/galeri), ukuran & posisi turntable, gaya vinyl & tonearm, sampul, RPM, jam (5 gaya, font, warna, ukuran, posisi, 12/24 jam), widget kalender/cuaca/baterai yang bisa diseret & diubah ukuran, gestur, preset tema |
| Musik | NotificationListenerService + MediaSessionManager; memprioritaskan sesi yang sedang memutar; aplikasi musik default |
| App drawer | Swipe ke atas, grid abjad, pencarian; tekan lama: info, uninstall, tambah ke dock, sembunyikan |
| Dock | Default memakai taskbar sistem (konten diberi ruang aman lewat WindowInsets). Opsional: dock vinyl mini 6–8 aplikasi |

## Cara build di Android Studio

1. Pasang **Android Studio Narwhal (2025.1) atau lebih baru** dengan **Android SDK Platform 36** dan **JDK 17** (JBR bawaan Android Studio sudah cukup).
2. Ekstrak `VinylLauncher.zip`, lalu **File → Open** dan pilih folder `VinylLauncher`.
3. Tunggu *Gradle Sync* selesai (Gradle 8.13 dan semua dependensi diunduh otomatis). Jika Android Studio menawarkan upgrade AGP/Kotlin, aman untuk diterima.
4. Sambungkan tablet (aktifkan **Opsi pengembang → USB debugging**) atau jalankan emulator tablet (mis. *Pixel Tablet*, API 34+).
5. Klik **Run ▶** (konfigurasi `app`).

Build lewat terminal:

```bash
./gradlew assembleDebug          # APK di app/build/outputs/apk/debug/
./gradlew installDebug           # langsung pasang ke perangkat
```

## Setelah terpasang

Aplikasi pertama kali membuka **onboarding**:

1. **Jadikan launcher default** — pilih Vinyl Launcher sebagai aplikasi Home.
2. **Akses notifikasi** — wajib agar launcher bisa membaca & mengontrol lagu (isi notifikasi tidak dibaca).
3. **Kalender** *(opsional)* — untuk titik acara di kalender layar kunci.
4. **Layar kunci vinyl** *(opsional)* — izinkan "Tampilkan di atas aplikasi lain" & notifikasi, lalu aktifkan.
5. **Aksi gestur** *(opsional)* — layanan aksesibilitas untuk gestur "kunci layar" dan "buka notifikasi".

Ikon *Vinyl Launcher* di app drawer membuka halaman **Pengaturan** (semua opsi + reset ke default).

## Catatan teknis

- **Arsitektur:** MVVM — `data/` (DataStore, LauncherApps, kalender, cuaca), `media/` (MediaSession), `service/`, `ui/` (layar + ViewModel), `ui/components/` (komponen Canvas).
- **Penyimpanan:** seluruh pengaturan disimpan sebagai satu dokumen JSON (kotlinx.serialization) di DataStore, sehingga field baru otomatis memakai default dan reset/preset mudah.
- **Grafis:** vinyl, alur, kilap, label melingkar, tonearm, jam analog/melingkar, dan ikon dock digambar dengan Compose Canvas — tajam di semua ukuran. Loop putaran memakai `withFrameNanos` dan berhenti sendiri saat vinyl diam (hemat baterai).
- **Rotasi layar:** activity menangani `configChanges` sendiri; tata letak dihitung ulang dari `BoxWithConstraints`, posisi widget disimpan sebagai fraksi layar.
- **Layar kunci:** `CoverActivity` memakai `setShowWhenLocked` + `setTurnScreenOn`; keamanan tetap ditangani PIN/sandi/biometrik sistem via `KeyguardManager.requestDismissKeyguard`. Activity dibuka oleh `CoverService` (foreground service ringan) saat layar mati. Android 10+ membatasi pembukaan activity dari latar belakang, karena itu izin *Tampilkan di atas aplikasi lain* diperlukan.
- **Cuaca:** Open-Meteo (gratis, tanpa API key), lokasi diatur manual di Pengaturan → Cuaca; di-cache 30 menit.
- **Gestur notifikasi/kunci layar:** memakai layanan aksesibilitas bila aktif; notifikasi punya cadangan lewat `StatusBarManager` (API tersembunyi, berfungsi di sebagian besar perangkat).

## Keterbatasan yang perlu diketahui

- Beberapa ROM (mis. One UI, HyperOS) menghilangkan taskbar saat launcher pihak ketiga aktif — aktifkan **Dock vinyl mini** di Pengaturan.
- Seek (seret tonearm / putar vinyl) hanya aktif bila aplikasi musik mendukung `ACTION_SEEK_TO`.
- Cover yang hanya dikirim sebagai URL http(s) tidak diunduh; mayoritas aplikasi musik mengirim bitmap langsung di metadata.
- Proyek ini belum di-build di lingkungan pembuatnya (tanpa akses Android SDK); bila ada error kompilasi kecil saat sync pertama, pesan Gradle akan menunjuk baris yang tepat.
