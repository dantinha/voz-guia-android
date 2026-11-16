package br.com.vozguia.core.feedback

/**
 * Eventos que exigem feedback háptico e sonoro imediato (< 50ms)
 * para eliminar a sensação de silêncio no escuro.
 */
enum class FeedbackEvent {
    /** Microfone aberto para captura de comando de voz (< 40ms) */
    LISTENING_START,

    /** Fim da fala do usuário captada pelo VAD; início do processamento (< 40ms) */
    THINKING_START,

    /** Pulso periódico (a cada 1000ms) se o processamento passar de 1s */
    HEARTBEAT,

    /** Ação executada com sucesso (mensagem enviada, alarme criado, etc.) */
    SUCCESS,

    /** Comando cancelado pelo usuário ou botão de pânico acionado (< 30ms) */
    CANCELLED,

    /** Notificação sonora e tátil ao acender a tela do celular */
    WAKEUP
}

/**
 * Padrão imutável de vibração para uso com VibrationEffect.createWaveform.
 */
data class HapticPattern(
    val timings: LongArray,
    val amplitudes: IntArray,
    val repeatIndex: Int = -1
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as HapticPattern
        return timings.contentEquals(other.timings) &&
                amplitudes.contentEquals(other.amplitudes) &&
                repeatIndex == other.repeatIndex
    }

    override fun hashCode(): Int {
        var result = timings.contentHashCode()
        result = 31 * result + amplitudes.contentHashCode()
        result = 31 * result + repeatIndex
        return result
    }
}

/**
 * Padrão sonoro sintético ou recurso de áudio.
 */
data class AudioTonePattern(
    val frequenciesHz: IntArray,
    val durationsMs: IntArray,
    val volume: Float = 1.0f
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as AudioTonePattern
        return frequenciesHz.contentEquals(other.frequenciesHz) &&
                durationsMs.contentEquals(other.durationsMs) &&
                volume == other.volume
    }

    override fun hashCode(): Int {
        var result = frequenciesHz.contentHashCode()
        result = 31 * result + durationsMs.contentHashCode()
        result = 31 * result + volume.hashCode()
        return result
    }
}

/**
 * Mapeador central das assinaturas sensoriais do Voz Guia.
 */
object FeedbackMapper {

    fun getHapticPattern(event: FeedbackEvent): HapticPattern {
        return when (event) {
            FeedbackEvent.LISTENING_START -> HapticPattern(
                timings = longArrayOf(0L, 50L),
                amplitudes = intArrayOf(0, 180)
            )

            FeedbackEvent.THINKING_START -> HapticPattern(
                timings = longArrayOf(0L, 25L),
                amplitudes = intArrayOf(0, 100)
            )

            FeedbackEvent.HEARTBEAT -> HapticPattern(
                timings = longArrayOf(0L, 15L),
                amplitudes = intArrayOf(0, 60)
            )

            FeedbackEvent.SUCCESS -> HapticPattern(
                timings = longArrayOf(0L, 40L, 60L, 60L),
                amplitudes = intArrayOf(0, 200, 0, 200)
            )

            FeedbackEvent.CANCELLED -> HapticPattern(
                timings = longArrayOf(0L, 80L),
                amplitudes = intArrayOf(0, 150)
            )

            FeedbackEvent.WAKEUP -> HapticPattern(
                timings = longArrayOf(0L, 30L),
                amplitudes = intArrayOf(0, 120)
            )
        }
    }

    fun getAudioTone(event: FeedbackEvent): AudioTonePattern {
        return when (event) {
            FeedbackEvent.LISTENING_START -> AudioTonePattern(
                frequenciesHz = intArrayOf(523, 784), // Dó5 (523Hz) -> Sol5 (784Hz)
                durationsMs = intArrayOf(30, 40),
                volume = 0.8f
            )

            FeedbackEvent.THINKING_START -> AudioTonePattern(
                frequenciesHz = intArrayOf(659), // Mi5
                durationsMs = intArrayOf(25),
                volume = 0.4f
            )

            FeedbackEvent.HEARTBEAT -> AudioTonePattern(
                frequenciesHz = intArrayOf(440), // Lá4 (tick suave)
                durationsMs = intArrayOf(15),
                volume = 0.2f
            )

            FeedbackEvent.SUCCESS -> AudioTonePattern(
                frequenciesHz = intArrayOf(523, 659, 784), // Acorde Dó-Mi-Sol
                durationsMs = intArrayOf(40, 40, 80),
                volume = 0.9f
            )

            FeedbackEvent.CANCELLED -> AudioTonePattern(
                frequenciesHz = intArrayOf(392, 261), // Sol4 -> Dó4 (descendente)
                durationsMs = intArrayOf(40, 60),
                volume = 0.7f
            )

            FeedbackEvent.WAKEUP -> AudioTonePattern(
                frequenciesHz = intArrayOf(587), // Ré5
                durationsMs = intArrayOf(30),
                volume = 0.5f
            )
        }
    }
}
