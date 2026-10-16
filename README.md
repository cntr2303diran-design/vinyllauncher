# Vinyl Launcher

Launcher Android untuk tablet dengan konsep **"tablet sebagai turntable"** — Kotlin + Jetpack Compose, minSdk 29, targetSdk 36. Dioptimalkan untuk landscape; portrait tetap didukung dengan tata letak yang menyesuaikan.

## Fitur (versi 1.0)

| Bagian | Isi |
|---|---|
| Layar utama | Latar ambient dari warna cover, turntable Canvas (sampul, vinyl beralur & berkilap, tonearm), jam & widget yang bisa diatur, info lagu + lirik tersinkron (LRCLIB) |
| Animasi | Play: vinyl meluncur keluar → tonearm turun → berputar 33/45 RPM. Pause: tonearm naik → melambat → masuk sebagian ke sampul |
| Interaksi turntable | Ketuk sampul = putar/jeda · geser sampul = lagu berikut/sebelumnya · seret tonearm = seek · angkat tonearm dari piringan = jeda · putar vinyl dengan jari = maju/mundur |
| Suara & getar | Bunyi jarum, klik saat vinyl diputar, kibasan ganti lagu, kresek vinyl opsional; volume & kekuatan getar bisa diatur |
| Rak piringan | Geser ke kiri di layar utama: cover-flow di atas papan kayu dengan piringan menyembul, lajur *Baru diputar*, tab **Riwayat** per hari, tab **Koleksi** dengan statistik |
| Layar kunci vinyl | Tampil di atas keyguard, jam, tanggal, kalender, kartu lagu + lirik; geser untuk membuka, tekan lama untuk editor |
| Taskbar Vinyl | Bar gaya PC di atas semua aplikasi: Home, panel semua aplikasi, favorit, aplikasi terakhir, kartu lagu, jam/tanggal/baterai; posisi bawah/atas/kiri/kanan; opsi buka aplikasi sebagai jendela |
| Tombol navigasi Vinyl | Kembali/Home/Terbaru melayang: posisi bebas, jarak antar tombol, ukuran, transparansi, urutan, susun vertikal |
| Gestur | Swipe atas/bawah & ketuk dua kali, plus **gestur gambar sendiri** (satu goresan) dengan 16 pilihan aksi |
| Kustomisasi | Tekan lama area kosong: latar, turntable, jam (5 gaya), widget kalender/cuaca/baterai/foto, gestur, preset tema |
| Pengaturan | Dua panel ala tablet (kategori di kiri, isi di kanan) |

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

## Unduh APK tanpa laptop

Setiap perubahan di branch `main` otomatis di-build oleh GitHub Actions. APK terbaru selalu tersedia di:
`https://github.com/cntr2303diran-design/vinyllauncher/releases/download/latest/VinylLauncher.apk`

## Catatan teknis

- **Arsitektur:** MVVM — `data/` (DataStore, LauncherApps, kalender, cuaca), `media/` (MediaSession), `service/`, `ui/` (layar + ViewModel), `ui/components/` (komponen Canvas).
- **Penyimpanan:** seluruh pengaturan disimpan sebagai satu dokumen JSON (kotlinx.serialization) di DataStore, sehingga field baru otomatis memakai default dan reset/preset mudah.
- **Grafis:** vinyl, alur, kilap, label melingkar, tonearm, jam analog/melingkar, dan ikon dock digambar dengan Compose Canvas — tajam di semua ukuran. Loop putaran memakai `withFrameNanos` dan berhenti sendiri saat vinyl diam (hemat baterai).
- **Rotasi layar:** activity menangani `configChanges` sendiri; tata letak dihitung ulang dari `BoxWithConstraints`, posisi widget disimpan sebagai fraksi layar.
- **Layar kunci:** `CoverActivity` memakai `setShowWhenLocked` + `setTurnScreenOn`; keamanan tetap ditangani PIN/sandi/biometrik sistem via `KeyguardManager.requestDismissKeyguard`. Activity dibuka oleh `CoverService` (foreground service ringan) saat layar mati. Android 10+ membatasi pembukaan activity dari latar belakang, karena itu izin *Tampilkan di atas aplikasi lain* diperlukan.
- **Font:** Inter, Instrument Serif, dan JetBrains Mono (lisensi OFL) diunduh otomatis dari repositori google/fonts saat build pertama (task Gradle `fetchFonts`), jadi build pertama butuh internet.
- **Cuaca:** Open-Meteo (gratis, tanpa API key), lokasi diatur manual di Pengaturan → Cuaca; di-cache 30 menit.
- **Gestur notifikasi/kunci layar:** memakai layanan aksesibilitas bila aktif; notifikasi punya cadangan lewat `StatusBarManager` (API tersembunyi, berfungsi di sebagian besar perangkat).

## Keterbatasan yang perlu diketahui

- One UI menonaktifkan taskbar Samsung saat launcher pihak ketiga aktif — gunakan **Taskbar Vinyl** (Pengaturan → Taskbar).
- Tombol navigasi Vinyl memakai layanan aksesibilitas untuk Kembali & Terbaru. Agar tombol bawaan hilang, ubah navigasi sistem ke *Gestur geser*.
- "Buka sebagai jendela" hanya berfungsi bila sistem mengizinkan jendela bentuk bebas (Opsi pengembang).
- Seek (seret tonearm / putar vinyl) hanya aktif bila aplikasi musik mendukung `ACTION_SEEK_TO`.
- Gestur gambar sendiri dikenali di area kosong layar utama (bukan di dalam aplikasi lain).
