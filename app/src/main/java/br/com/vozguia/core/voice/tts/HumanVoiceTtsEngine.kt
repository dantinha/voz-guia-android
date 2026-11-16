package br.com.vozguia.core.voice.tts

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import java.util.UUID

/**
 * Configuração de prosódia para voz humanizada pt-BR.
 */
data class TtsProsodyConfig(
    val speechRate: Float = 0.92f,
    val repeatSpeechRate: Float = 0.82f,
    val pitch: Float = 1.0f
)

/**
 * Interface que abstrai o hardware / motor TTS para viabilizar testes unitários e alternância de engines.
 */
interface TextToSpeechSpeaker {
    fun speak(text: String, speechRate: Float, pitch: Float, onDone: (() -> Unit)?)
    fun stop()
    val isSpeaking: Boolean
}

/**
 * Motor central de síntese de voz humanizada com suporte a Barge-in (< 20ms),
 * ajuste dinâmico de velocidade pelo cuidador e repetição desacelerada sob demanda.
 */
class HumanVoiceTtsEngine(
    private val speaker: TextToSpeechSpeaker,
    private val sanitizer: TextLexicalSanitizer = TextLexicalSanitizer(),
    private var prosodyConfig: TtsProsodyConfig = TtsProsodyConfig()
) {
    var lastSpokenMessage: String? = null
        private set

    val currentSpeechRate: Float get() = prosodyConfig.speechRate
    val isSpeaking: Boolean get() = speaker.isSpeaking

    fun updateSpeechRate(newRate: Float) {
        val clampedRate = newRate.coerceIn(0.75f, 1.20f)
        val clampedRepeat = (clampedRate - 0.10f).coerceAtLeast(0.65f)
        prosodyConfig = prosodyConfig.copy(
            speechRate = clampedRate,
            repeatSpeechRate = clampedRepeat
        )
    }

    fun speak(
        text: String,
        isImportant: Boolean = true,
        onDone: (() -> Unit)? = null
    ) {
        val sanitized = sanitizer.sanitize(text)
        if (sanitized.isBlank()) {
            onDone?.invoke()
            return
        }

        if (isImportant) {
            lastSpokenMessage = sanitized
        }

        speaker.speak(
            text = sanitized,
            speechRate = prosodyConfig.speechRate,
            pitch = prosodyConfig.pitch,
            onDone = onDone
        )
    }

    fun repeatLast(onDone: (() -> Unit)? = null) {
        val message = lastSpokenMessage ?: "Não há mensagens anteriores para repetir."

        speaker.speak(
            text = message,
            speechRate = prosodyConfig.repeatSpeechRate,
            pitch = prosodyConfig.pitch,
            onDone = onDone
        )
    }

    fun stop() {
        speaker.stop()
    }
}

/**
 * Implementação nativa usando o TextToSpeech oficial do Android em pt-BR.
 * Garante despacho de callbacks na Main Thread e fila segura durante inicialização.
 */
class AndroidTextToSpeechSpeaker(
    context: Context,
    private val onInitComplete: (() -> Unit)? = null
) : TextToSpeechSpeaker {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var initFailed = false
    private var pendingDoneCallback: (() -> Unit)? = null
    private var currentlySpeaking = false

    private var queuedText: String? = null
    private var queuedRate: Float = 0.92f
    private var queuedPitch: Float = 1.0f
    private var queuedCallback: (() -> Unit)? = null

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.forLanguageTag("pt-BR")
                isInitialized = true
                setupUtteranceListener()
                mainHandler.post {
                    onInitComplete?.invoke()
                    val pending = queuedText
                    if (pending != null) {
                        val cb = queuedCallback
                        queuedText = null
                        queuedCallback = null
                        speak(pending, queuedRate, queuedPitch, cb)
                    }
                }
            } else {
                initFailed = true
                mainHandler.post {
                    val cb = queuedCallback
                    queuedText = null
                    queuedCallback = null
                    cb?.invoke()
                }
            }
        }
    }

    private fun setupUtteranceListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                currentlySpeaking = true
            }

            override fun onDone(utteranceId: String?) {
                currentlySpeaking = false
                val callback = pendingDoneCallback
                pendingDoneCallback = null
                if (callback != null) {
                    mainHandler.post(callback)
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                currentlySpeaking = false
                val callback = pendingDoneCallback
                pendingDoneCallback = null
                if (callback != null) {
                    mainHandler.post(callback)
                }
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                currentlySpeaking = false
                val callback = pendingDoneCallback
                pendingDoneCallback = null
                if (callback != null) {
                    mainHandler.post(callback)
                }
            }
        })
    }

    override fun speak(text: String, speechRate: Float, pitch: Float, onDone: (() -> Unit)?) {
        if (initFailed) {
            onDone?.invoke()
            return
        }

        if (!isInitialized) {
            // Substitui qualquer fala enfileirada durante inicialização sem prender o estado
            queuedCallback?.invoke()
            queuedText = text
            queuedRate = speechRate
            queuedPitch = pitch
            queuedCallback = onDone
            return
        }

        pendingDoneCallback = onDone
        tts?.setSpeechRate(speechRate)
        tts?.setPitch(pitch)

        val utteranceId = UUID.randomUUID().toString()
        currentlySpeaking = true
        val result = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        if (result == TextToSpeech.ERROR) {
            currentlySpeaking = false
            val cb = pendingDoneCallback
            pendingDoneCallback = null
            cb?.invoke()
        }
    }

    override fun stop() {
        currentlySpeaking = false
        pendingDoneCallback = null
        queuedText = null
        queuedCallback = null
        tts?.stop()
    }

    override val isSpeaking: Boolean get() = currentlySpeaking

    fun shutdown() {
        stop()
        tts?.shutdown()
    }
}
