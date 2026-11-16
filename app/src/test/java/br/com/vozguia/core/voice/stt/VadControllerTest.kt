package br.com.vozguia.core.voice.stt

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VadControllerTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var vadController: VadController

    @Before
    fun setUp() {
        vadController = VadController(
            coroutineScope = testScope,
            config = VadConfig(
                initialSpeechTimeoutMs = 4500L,
                shortCommandSilenceMs = 900L,
                dictationSilenceMs = 1500L,
                antiEchoCooldownMs = 150L
            )
        )
    }

    @Test
    fun `quando entrar em escuta e usuario nao falar nada, emite timeout suave aos 4500ms`() = testScope.runTest {
        var timeoutFired = false
        vadController.onNoSpeechTimeout = { timeoutFired = true }

        vadController.startListening(isDictation = false)

        advanceTimeBy(4499L)
        assertThat(timeoutFired).isFalse()

        advanceTimeBy(2L)
        assertThat(timeoutFired).isTrue()
    }

    @Test
    fun `em comando curto, encerra escuta apos 900ms de silencio continuo`() = testScope.runTest {
        var speechFinished = false
        vadController.onSpeechCompleted = { speechFinished = true }

        vadController.startListening(isDictation = false)

        // Usuário começa a falar após 500ms
        advanceTimeBy(500L)
        vadController.onVoiceActivityDetected()

        // Fala dura 1000ms
        advanceTimeBy(1000L)
        vadController.onSilenceDetected()

        // 899ms de silêncio (ainda não fechou)
        advanceTimeBy(899L)
        assertThat(speechFinished).isFalse()

        // Atinge 901ms de silêncio
        advanceTimeBy(2L)
        assertThat(speechFinished).isTrue()
    }

    @Test
    fun `em modo ditado de mensagem, tolera pausa maior de ate 1500ms antes de fechar`() = testScope.runTest {
        var speechFinished = false
        vadController.onSpeechCompleted = { speechFinished = true }

        vadController.startListening(isDictation = true)

        vadController.onVoiceActivityDetected()
        advanceTimeBy(1000L)
        vadController.onSilenceDetected()

        // Aos 1000ms de silêncio (em ditado NÃO deve fechar ainda)
        advanceTimeBy(1000L)
        assertThat(speechFinished).isFalse()

        // Aos 1501ms de silêncio (fecha ditado com sucesso)
        advanceTimeBy(501L)
        assertThat(speechFinished).isTrue()
    }
}
