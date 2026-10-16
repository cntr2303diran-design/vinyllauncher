package com.vinyl.launcher.service

import android.service.notification.NotificationListenerService
import com.vinyl.launcher.media.MediaRepository

/**
 * NotificationListenerService hanya dipakai sebagai "kunci" agar launcher
 * boleh membaca sesi media aktif lewat MediaSessionManager.
 * Isi notifikasi tidak dibaca maupun disimpan.
 */
class MediaListenerService : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        MediaRepository.init(this)
        MediaRepository.connect()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        MediaRepository.disconnect()
    }
}
