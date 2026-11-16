package br.com.vozguia.core.voice.tts

import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

class HumanVoiceTtsEngineTest {

    private lateinit var fakeSpeaker: FakeTextToSpeechSpeaker
    private lateinit var ttsEngine: HumanVoiceTtsEngine

    @Before
    fun setUp() {
        fakeSpeaker = FakeTextToSpeechSpeaker()
        ttsEngine = HumanVoiceTtsEngine(
            speaker = fakeSpeaker,
            sanitizer = TextLexicalSanitizer(),
            prosodyConfig = TtsProsodyConfig(
                speechRate = 0.92f,
                repeatSpeechRate = 0.82f,
                pitch = 1.0f
            )
        )
    }

    @Test
    fun `speak deve sanitizar texto, aplicar prosodia 0_92x e salvar na memoria`() {
        ttsEngine.speak(
            text = "Botão não marcado. O Gabriel mandou um áudio.",
            isImportant = true
        )

        assertThat(fakeSpeaker.lastSpokenText).isEqualTo("O Gabriel mandou um áudio.")
        assertThat(fakeSpeaker.appliedSpeechRate).isEqualTo(0.92f)
        assertThat(ttsEngine.lastSpokenMessage).isEqualTo("O Gabriel mandou um áudio.")
    }

    @Test
    fun `repeatLast deve repetir ultima mensagem importante com velocidade reduzida em menos 10 por cento`() {
        ttsEngine.speak(
            text = "Gabriel: Já almoçou, pai?",
            isImportant = true
        )

        // Usuário pede para repetir
        ttsEngine.repeatLast()

        assertThat(fakeSpeaker.lastSpokenText).isEqualTo("Gabriel: Já almoçou, pai?")
        assertThat(fakeSpeaker.appliedSpeechRate).isEqualTo(0.82f)
    }

    @Test
    fun `stop deve parar a fala imediatamente com barge-in`() {
        ttsEngine.speak("Texto longo sendo falado...")
        assertThat(ttsEngine.isSpeaking).isTrue()

        ttsEngine.stop()

        assertThat(fakeSpeaker.isStopped).isTrue()
        assertThat(ttsEngine.isSpeaking).isFalse()
    }

    @Test
    fun `updateSpeechRate deve aplicar nova velocidade dentro dos limites seguros e ajustar repeatRate`() {
        ttsEngine.updateSpeechRate(1.10f)
        ttsEngine.speak("Teste de velocidade ajustada pelo Gabriel.")
        assertThat(fakeSpeaker.appliedSpeechRate).isWithin(0.001f).of(1.10f)

        ttsEngine.updateSpeechRate(2.5f) // acima do limite
        ttsEngine.speak("Limitado ao teto seguro.")
        assertThat(fakeSpeaker.appliedSpeechRate).isWithin(0.001f).of(1.20f)
    }

    private class FakeTextToSpeechSpeaker : TextToSpeechSpeaker {
        var lastSpokenText: String? = null
        var appliedSpeechRate: Float = 1.0f
        var isStopped: Boolean = false
        var active: Boolean = false

        override fun speak(text: String, speechRate: Float, pitch: Float, onDone: (() -> Unit)?) {
            lastSpokenText = text
            appliedSpeechRate = speechRate
            isStopped = false
            active = true
            // Callback pode ser disparado em testes quando necessário
        }

        override fun stop() {
            isStopped = true
            active = false
        }

        override val isSpeaking: Boolean get() = active
    }
}
