package br.com.vozguia.core.feedback

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
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

/**
 * Interface do motor sensorial de resposta física e sonora instantânea (< 50ms).
 */
interface HapticAudioFeedbackEngine {
    fun trigger(event: FeedbackEvent)
    fun startHeartbeat()
    fun stopHeartbeat()
}

/**
 * Implementação nativa para Android usando Vibrator / VibrationEffect e síntese PCM de baixa latência.
 */
class AndroidHapticAudioFeedbackEngine(
    private val context: Context,
    private val coroutineScope: CoroutineScope
) : HapticAudioFeedbackEngine {

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    private val heartbeatController = HeartbeatController(
        coroutineScope = coroutineScope,
        intervalMs = 1000L,
        initialDelayMs = 1000L,
        onHeartbeat = { trigger(FeedbackEvent.HEARTBEAT) }
    )

    override fun trigger(event: FeedbackEvent) {
        val pattern = FeedbackMapper.getHapticPattern(event)
        playHaptic(pattern)
        playAudio(event)
    }

    override fun startHeartbeat() {
        heartbeatController.start()
    }

    override fun stopHeartbeat() {
        heartbeatController.stop()
    }

    private fun playHaptic(pattern: HapticPattern) {
        if (vibrator?.hasVibrator() != true) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createWaveform(
                    pattern.timings,
                    pattern.amplitudes,
                    pattern.repeatIndex
                )
                vibrator?.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern.timings, pattern.repeatIndex)
            }
        } catch (_: Exception) {
            // Falha segura se hardware não suportar waveform
        }
    }

    private fun playAudio(event: FeedbackEvent) {
        val tone = FeedbackMapper.getAudioTone(event)
        coroutineScope.launch(Dispatchers.Default) {
            playSynthesizedTone(tone)
        }
    }

    private fun playSynthesizedTone(tone: AudioTonePattern) {
        val sampleRate = 22050
        val count = min(tone.frequenciesHz.size, tone.durationsMs.size)
        if (count == 0) return

        val totalSamples = (0 until count).sumOf { (tone.durationsMs[it] * sampleRate) / 1000 }
        if (totalSamples <= 0) return

        val pcmBuffer = ShortArray(totalSamples)
        var offset = 0
        val maxAmplitude = (Short.MAX_VALUE * tone.volume.coerceIn(0f, 1f) * 0.45f)

        for (seg in 0 until count) {
            val freq = tone.frequenciesHz[seg].toDouble()
            val segSamples = (tone.durationsMs[seg] * sampleRate) / 1000
            val fadeSamples = min(segSamples / 5, 110)

            for (i in 0 until segSamples) {
                val angle = 2.0 * PI * i * freq / sampleRate
                var env = 1.0
                if (fadeSamples > 0) {
                    if (i < fadeSamples) env = i.toDouble() / fadeSamples
                    else if (i > segSamples - fadeSamples) env = (segSamples - i).toDouble() / fadeSamples
                }
                pcmBuffer[offset + i] = (sin(angle) * maxAmplitude * env).toInt().toShort()
            }
            offset += segSamples
        }

        try {
            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val format = AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(sampleRate)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()

            val track = AudioTrack.Builder()
                .setAudioAttributes(attributes)
                .setAudioFormat(format)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(pcmBuffer.size * 2)
                .build()

            track.write(pcmBuffer, 0, pcmBuffer.size)
            track.play()
            val totalDurationMs = tone.durationsMs.take(count).sum().toLong() + 40L
            Thread.sleep(totalDurationMs)
            track.stop()
            track.release()
        } catch (_: Exception) {
            // Proteção caso recurso de áudio esteja indisponível
        }
    }
}
