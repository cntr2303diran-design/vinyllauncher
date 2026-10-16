package com.vinyl.launcher.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.math.abs

data class LyricLine(val timeMs: Long, val text: String)

/** Lirik sebuah lagu: [synced] berisi baris bertanda waktu (kosong jika hanya ada lirik biasa). */
data class Lyrics(val synced: List<LyricLine>, val plain: String?) {
    val hasSynced get() = synced.isNotEmpty()

    /** Indeks baris yang sedang dinyanyikan pada posisi [ms], atau -1 sebelum baris pertama. */
    fun indexAt(ms: Long): Int {
        if (synced.isEmpty() || ms < synced.first().timeMs) return -1
        var lo = 0
        var hi = synced.lastIndex
        while (lo < hi) {
            val mid = (lo + hi + 1) / 2
            if (synced[mid].timeMs <= ms) lo = mid else hi = mid - 1
        }
        return lo
    }
}

/**
 * Lirik tersinkron dari LRCLIB (lrclib.net) — database lirik terbuka, gratis, tanpa API key.
 * Hasil di-cache di memori per lagu agar tidak meminta ulang.
 */
object LyricsRepository {
    private const val UA = "VinylLauncher/1.0 (https://github.com/cntr2303diran-design/vinyllauncher)"
    private val cache = object : LinkedHashMap<String, Lyrics?>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Lyrics?>?) = size > 60
    }
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun get(title: String, artist: String, album: String, durationMs: Long): Lyrics? {
        if (title.isBlank()) return null
        val key = "$title|$artist"
        synchronized(cache) { if (cache.containsKey(key)) return cache[key] }
        val result = withContext(Dispatchers.IO) {
            runCatching { fetchExact(title, artist, album, durationMs) }.getOrNull()
                ?: runCatching { fetchSearch(clean(title), clean(artist), durationMs) }.getOrNull()
        }
        synchronized(cache) { cache[key] = result }
        return result
    }

    /** Buang embel-embel yang sering membuat pencarian gagal: "(feat. …)", "- Remastered 2011", dll. */
    private fun clean(s: String) = s
        .replace(Regex("""\s*[(\[](feat|ft|with|remaster|live|official|lyric|audio|video)[^)\]]*[)\]]""", RegexOption.IGNORE_CASE), "")
        .replace(Regex("""\s+-\s+.*(remaster|version|edit|mix|live).*$""", RegexOption.IGNORE_CASE), "")
        .trim()

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    private fun request(url: String): String? {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 7000; readTimeout = 9000
            setRequestProperty("User-Agent", UA)
        }
        return try {
            if (conn.responseCode == 200) conn.inputStream.bufferedReader().use { it.readText() } else null
        } finally {
            conn.disconnect()
        }
    }

    private fun fetchExact(title: String, artist: String, album: String, durationMs: Long): Lyrics? {
        var url = "https://lrclib.net/api/get?track_name=${enc(title)}&artist_name=${enc(artist)}"
        if (album.isNotBlank()) url += "&album_name=${enc(album)}"
        if (durationMs > 0) url += "&duration=${durationMs / 1000}"
        val body = request(url) ?: return null
        return parse(json.parseToJsonElement(body) as JsonObject)
    }

    private fun fetchSearch(title: String, artist: String, durationMs: Long): Lyrics? {
        val body = request("https://lrclib.net/api/search?track_name=${enc(title)}&artist_name=${enc(artist)}") ?: return null
        val arr = json.parseToJsonElement(body) as? JsonArray ?: return null
        val candidates = arr.mapNotNull { it as? JsonObject }
        // Utamakan yang punya lirik tersinkron & durasinya paling dekat
        val best = candidates.sortedWith(
            compareBy<JsonObject>({ it["syncedLyrics"]?.jsonPrimitive?.contentOrNull.isNullOrBlank() },
                { o -> if (durationMs > 0) abs((o["duration"]?.jsonPrimitive?.doubleOrNull ?: 0.0) * 1000 - durationMs) else 0.0 })
        ).firstOrNull() ?: return null
        return parse(best)
    }

    private fun parse(o: JsonObject): Lyrics? {
        val synced = o["syncedLyrics"]?.jsonPrimitive?.contentOrNull
        val plain = o["plainLyrics"]?.jsonPrimitive?.contentOrNull
        val lines = synced?.let { parseLrc(it) }.orEmpty()
        if (lines.isEmpty() && plain.isNullOrBlank()) return null
        return Lyrics(lines, plain)
    }

    /** Parser format LRC: "[mm:ss.xx] teks" (satu baris bisa punya beberapa tanda waktu). */
    fun parseLrc(lrc: String): List<LyricLine> {
        val tag = Regex("""\[(\d{1,2}):(\d{2})(?:[.:](\d{1,3}))?]""")
        return lrc.lines().flatMap { line ->
            val tags = tag.findAll(line).toList()
            if (tags.isEmpty()) return@flatMap emptyList()
            val text = line.substring(tags.last().range.last + 1).trim()
            tags.map { m ->
                val (mm, ss, frac) = m.destructured
                val fracMs = when (frac.length) {
                    0 -> 0
                    1 -> frac.toInt() * 100
                    2 -> frac.toInt() * 10
                    else -> frac.take(3).toInt()
                }
                LyricLine(mm.toLong() * 60_000 + ss.toLong() * 1000 + fracMs, text)
            }
        }.sortedBy { it.timeMs }
    }
}
