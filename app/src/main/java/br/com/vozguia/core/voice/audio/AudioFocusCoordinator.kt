package br.com.vozguia.core.voice.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build

/**
 * Interface de baixo nível para requisição e liberação de foco de áudio do Android.
 */
interface AudioFocusHandler {
    fun requestTransientFocus(): Boolean
    fun abandonFocus(): Boolean
}

/**
 * Coordenador de AudioFocus para garantia de clareza auditiva.
 * Pausa ou reduz vídeos do YouTube e áudios do WhatsApp durante a fala/escuta do agente.
 */
class AudioFocusCoordinator(
    private val focusHandler: AudioFocusHandler
) {
    var isFocusHeld = false
        private set

    fun acquireFocusForVoiceInteraction(): Boolean {
        val granted = focusHandler.requestTransientFocus()
        if (granted) {
            isFocusHeld = true
        }
        return granted
    }

    fun releaseFocus(preservePause: Boolean = false) {
        if (!isFocusHeld) return

        if (!preservePause) {
            focusHandler.abandonFocus()
            isFocusHeld = false
        }
    }
}

/**
 * Implementação nativa usando AudioManager e AudioFocusRequest (Android O+).
 */
class AndroidAudioFocusHandler(
    context: Context
) : AudioFocusHandler {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var focusRequest: AudioFocusRequest? = null

    override fun requestTransientFocus(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val playbackAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()

            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                .setAudioAttributes(playbackAttributes)
                .setAcceptsDelayedFocusGain(false)
                .setOnAudioFocusChangeListener { /* No-op para assistente prioritário */ }
                .build()

            focusRequest = request
            return audioManager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            return audioManager.requestAudioFocus(
                { /* No-op */ },
                AudioManager.STREAM_ACCESSIBILITY,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    override fun abandonFocus(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val request = focusRequest ?: return true
            val result = audioManager.abandonAudioFocusRequest(request)
            focusRequest = null
            return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            return audioManager.abandonAudioFocus { } == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }
}
