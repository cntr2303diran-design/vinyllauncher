package com.vinyl.launcher.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/** Pemuat gambar mini (tanpa library): bitmap langsung, path file, content:// atau http(s), dengan cache memori. */
object RemoteImage {
    private val cache = LruCache<String, ImageBitmap>(80)

    suspend fun load(context: Context, source: String): ImageBitmap? {
        cache.get(source)?.let { return it }
        val bmp = withContext(Dispatchers.IO) {
            runCatching {
                val u = Uri.parse(source)
                when (u.scheme) {
                    "http", "https" -> {
                        val conn = (URL(source).openConnection() as HttpURLConnection).apply { connectTimeout = 6000; readTimeout = 8000 }
                        try { conn.inputStream.use { decodeSampled(it.readBytes()) } } finally { conn.disconnect() }
                    }
                    "content", "android.resource", "file" ->
                        context.contentResolver.openInputStream(u)?.use { decodeSampled(it.readBytes()) }
                    else -> BitmapFactory.decodeFile(source)
                }
            }.getOrNull()
        } ?: return null
        return bmp.asImageBitmap().also { cache.put(source, it) }
    }

    private fun decodeSampled(bytes: ByteArray, max: Int = 400): Bitmap? {
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, o)
        var s = 1
        while (maxOf(o.outWidth, o.outHeight) / (s * 2) >= max) s *= 2
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = s })
    }
}

/** Gambar dari bitmap (prioritas) atau sumber string (URI/path). */
@Composable
fun rememberImage(bitmap: Bitmap?, source: String?): ImageBitmap? {
    val context = LocalContext.current
    val img by produceState<ImageBitmap?>(bitmap?.asImageBitmap(), bitmap, source) {
        if (bitmap == null && source != null) value = RemoteImage.load(context, source)
    }
    return img
}
