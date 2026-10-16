package com.vinyl.launcher.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

data class WeatherInfo(val temperatureC: Double, val code: Int, val city: String) {
    /** Deskripsi singkat dari kode cuaca WMO. */
    val description: String get() = when (code) {
        0 -> "Cerah"
        1, 2 -> "Cerah berawan"
        3 -> "Berawan"
        45, 48 -> "Berkabut"
        in 51..57 -> "Gerimis"
        in 61..67, in 80..82 -> "Hujan"
        in 71..77, 85, 86 -> "Salju"
        in 95..99 -> "Badai petir"
        else -> "—"
    }
}

/**
 * Cuaca saat ini dari Open-Meteo (gratis, tanpa API key).
 * Hasil di-cache 30 menit agar hemat baterai & data.
 */
object WeatherRepository {
    private var cache: Pair<Long, WeatherInfo>? = null
    private var cacheKey: String? = null
    private const val TTL = 30 * 60 * 1000L

    suspend fun current(settings: WeatherSettings): WeatherInfo? = withContext(Dispatchers.IO) {
        val key = "${settings.latitude},${settings.longitude}"
        val now = System.currentTimeMillis()
        cache?.let { (t, w) -> if (cacheKey == key && now - t < TTL) return@withContext w }
        runCatching {
            val url = URL(
                String.format(
                    Locale.US,
                    "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f&current=temperature_2m,weather_code",
                    settings.latitude, settings.longitude,
                )
            )
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000; readTimeout = 8000
            }
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()
            val current = Json.parseToJsonElement(body).jsonObject["current"]!!.jsonObject
            WeatherInfo(
                temperatureC = current["temperature_2m"]!!.jsonPrimitive.double,
                code = current["weather_code"]!!.jsonPrimitive.int,
                city = settings.cityName,
            ).also { cache = now to it; cacheKey = key }
        }.getOrNull()
    }
}
