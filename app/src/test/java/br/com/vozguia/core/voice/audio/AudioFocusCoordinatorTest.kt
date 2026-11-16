package br.com.vozguia.core.voice.audio

import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

class AudioFocusCoordinatorTest {

    private lateinit var fakeFocusHandler: FakeAudioFocusHandler
    private lateinit var coordinator: AudioFocusCoordinator

    @Before
    fun setUp() {
        fakeFocusHandler = FakeAudioFocusHandler()
        coordinator = AudioFocusCoordinator(focusHandler = fakeFocusHandler)
    }

    @Test
    fun `ao entrar em escuta ou fala, requisita foco transiente exclusivo`() {
        val granted = coordinator.acquireFocusForVoiceInteraction()

        assertThat(granted).isTrue()
        assertThat(fakeFocusHandler.focusAcquired).isTrue()
    }

    @Test
    fun `ao finalizar interacao sem comando de pausa, abandona o foco para retomar midia`() {
        coordinator.acquireFocusForVoiceInteraction()
        assertThat(fakeFocusHandler.focusAcquired).isTrue()

        coordinator.releaseFocus(preservePause = false)
        assertThat(fakeFocusHandler.focusAcquired).isFalse()
    }

    @Test
    fun `se usuario pediu para pausar a midia, nao retoma o foco automaticamente`() {
        coordinator.acquireFocusForVoiceInteraction()

        coordinator.releaseFocus(preservePause = true)
        // Mantém foco retido para a música continuar pausada
        assertThat(fakeFocusHandler.focusAcquired).isTrue()
    }

    private class FakeAudioFocusHandler : AudioFocusHandler {
        var focusAcquired: Boolean = false

        override fun requestTransientFocus(): Boolean {
            focusAcquired = true
            return true
        }

        override fun abandonFocus(): Boolean {
            focusAcquired = false
            return true
        }
    }
}
