package br.com.vozguia.core.voice.stt

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Parâmetros de temporização do VAD (Voice Activity Detection) e Anti-Eco.
 */
data class VadConfig(
    val initialSpeechTimeoutMs: Long = 4500L,
    val shortCommandSilenceMs: Long = 900L,
    val dictationSilenceMs: Long = 1500L,
    val antiEchoCooldownMs: Long = 150L
)

/**
 * Controlador de VAD Adaptativo.
 * Respeita pausas naturais de idosos/deficientes visuais recentes,
 * aplicando janela mais longa para ditado de WhatsApp (1500ms) e rápida para comandos curtos (900ms).
 */
class VadController(
    private val coroutineScope: CoroutineScope,
    private val config: VadConfig = VadConfig()
) {
    var onSpeechCompleted: (() -> Unit)? = null
    var onNoSpeechTimeout: (() -> Unit)? = null

    private var initialTimeoutJob: Job? = null
    private var silenceJob: Job? = null
    private var isDictationMode = false
    private var hasDetectedVoice = false

    fun startListening(isDictation: Boolean = false) {
        stop()
        isDictationMode = isDictation
        hasDetectedVoice = false

        // Inicia timeout inicial aguardando o usuário começar a falar
        initialTimeoutJob = coroutineScope.launch {
            delay(config.initialSpeechTimeoutMs)
            if (!hasDetectedVoice) {
                onNoSpeechTimeout?.invoke()
            }
        }
    }

    fun onVoiceActivityDetected() {
        hasDetectedVoice = true
        initialTimeoutJob?.cancel()
        silenceJob?.cancel()
    }

    fun onSilenceDetected() {
        if (!hasDetectedVoice) return

        silenceJob?.cancel()
        val silenceThreshold = if (isDictationMode) {
            config.dictationSilenceMs
        } else {
            config.shortCommandSilenceMs
        }

        silenceJob = coroutineScope.launch {
            delay(silenceThreshold)
            onSpeechCompleted?.invoke()
        }
    }

    fun stop() {
        initialTimeoutJob?.cancel()
        silenceJob?.cancel()
        initialTimeoutJob = null
        silenceJob = null
        hasDetectedVoice = false
    }
}

/**
 * Coordenador de Anti-Eco Acústico (Anti-Self-Triggering).
 * Assegura que o microfone só abre após a fala do TTS terminar + 150ms de resfriamento.
 */
class AntiEchoCoordinator(
    private val coroutineScope: CoroutineScope,
    private val cooldownMs: Long = 150L
) {
    fun scheduleMicrophoneOpenAfterTts(
        flushAudioBuffer: () -> Unit,
        onReadyToListen: () -> Unit
    ) {
        coroutineScope.launch {
            delay(cooldownMs)
            flushAudioBuffer()
            onReadyToListen()
        }
    }
}
