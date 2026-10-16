package com.vinyl.launcher.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * Efek suara & getar turntable, disintesis langsung (tanpa file audio):
 * - [needleDown]: "tuk" jarum menyentuh piringan + desis & kresek vinyl selama ±2,5 detik
 * - [needleUp]: klik kecil saat lengan terangkat
 *
 * Suara diputar TANPA meminta audio focus, sehingga musik tidak terhenti.
 */
object SoundFx {
    private const val RATE = 44100
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Buffer dibuat sekali lalu dipakai ulang
    private val dropPcm: ShortArray by lazy { synthDrop() }
    private val liftPcm: ShortArray by lazy { synthLift() }

    fun needleDown(context: Context, sound: Boolean, haptics: Boolean, volume: Float) {
        if (haptics) vibrate(context, 18, 110)
        if (sound) play(dropPcm, volume)
    }

    fun needleUp(context: Context, sound: Boolean, haptics: Boolean, volume: Float) {
        if (haptics) vibrate(context, 10, 60)
        if (sound) play(liftPcm, volume * 0.8f)
    }

    private fun play(pcm: ShortArray, volume: Float) = scope.launch {
        runCatching {
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(pcm.size * 2)
                .build()
            track.write(pcm, 0, pcm.size)
            track.setVolume(volume.coerceIn(0f, 1f))
            track.play()
            // Lepas setelah selesai
            kotlinx.coroutines.delay(pcm.size * 1000L / RATE + 200)
            track.release()
        }
    }

    /** "Tuk" bernada rendah + klik tajam, lalu desis & kresek yang memudar. */
    private fun synthDrop(): ShortArray {
        val n = (RATE * 2.6).toInt()
        val out = ShortArray(n)
        val rnd = Random(33)
        var lp = 0.0
        for (i in 0 until n) {
            val t = i.toDouble() / RATE
            // thump 70 Hz yang cepat meredam
            val thump = sin(2 * PI * 70 * t) * exp(-t * 28) * 0.55
            // klik awal (noise sangat singkat)
            val click = if (t < 0.004) (rnd.nextDouble() * 2 - 1) * (1 - t / 0.004) * 0.6 else 0.0
            // desis vinyl: noise yang di-low-pass, memudar
            lp += 0.08 * ((rnd.nextDouble() * 2 - 1) - lp)
            val hiss = lp * 0.10 * exp(-t * 0.9) * (if (t > 0.05) 1.0 else t / 0.05)
            // kresek acak (impuls jarang)
            val crackle = if (rnd.nextDouble() < 0.0009 * exp(-t * 0.6)) (rnd.nextDouble() * 2 - 1) * 0.5 else 0.0
            val fadeOut = if (t > 2.2) (2.6 - t) / 0.4 else 1.0
            val v = (thump + click + hiss + crackle) * fadeOut
            out[i] = (v.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
        }
        return out
    }

    private fun synthLift(): ShortArray {
        val n = (RATE * 0.12).toInt()
        val rnd = Random(45)
        return ShortArray(n) { i ->
            val t = i.toDouble() / RATE
            val v = (rnd.nextDouble() * 2 - 1) * exp(-t * 90) * 0.35 + sin(2 * PI * 180 * t) * exp(-t * 60) * 0.2
            (v.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
        }
    }

    private fun vibrate(context: Context, ms: Long, amplitude: Int) {
        runCatching {
            val vib = if (Build.VERSION.SDK_INT >= 31)
                context.getSystemService(VibratorManager::class.java)?.defaultVibrator
            else @Suppress("DEPRECATION") context.getSystemService(Vibrator::class.java)
            if (vib?.hasVibrator() == true) {
                val amp = if (vib.hasAmplitudeControl()) amplitude else VibrationEffect.DEFAULT_AMPLITUDE
                vib.vibrate(VibrationEffect.createOneShot(ms, amp))
            }
        }
    }
}
