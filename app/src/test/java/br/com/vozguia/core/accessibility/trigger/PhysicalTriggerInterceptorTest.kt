package br.com.vozguia.core.accessibility.trigger

import br.com.vozguia.core.domain.AgentState
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PhysicalTriggerInterceptorTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var interceptor: PhysicalTriggerInterceptor

    private val KEYCODE_VOLUME_DOWN = 25
    private val KEYCODE_VOLUME_UP = 24

    @Before
    fun setUp() {
        interceptor = PhysicalTriggerInterceptor(
            coroutineScope = testScope,
            holdThresholdMs = 900L,
            tapThresholdMs = 500L
        )
    }

    @Test
    fun `quando estado for SPEAKING e usuario apertar Volume Menos, aciona PanicCancel instantaneamente`() {
        val action = interceptor.onKeyEvent(
            keyCode = KEYCODE_VOLUME_DOWN,
            isKeyDown = true,
            eventTimeMs = 1000L,
            currentState = AgentState.SPEAKING
        )

        assertThat(action).isEqualTo(TriggerAction.PanicCancel)
    }

    @Test
    fun `quando estado for LISTENING ou THINKING e apertar Volume Menos, aciona PanicCancel`() {
        val listeningAction = interceptor.onKeyEvent(
            keyCode = KEYCODE_VOLUME_DOWN,
            isKeyDown = true,
            eventTimeMs = 1000L,
            currentState = AgentState.LISTENING
        )
        assertThat(listeningAction).isEqualTo(TriggerAction.PanicCancel)

        val thinkingAction = interceptor.onKeyEvent(
            keyCode = KEYCODE_VOLUME_DOWN,
            isKeyDown = true,
            eventTimeMs = 1000L,
            currentState = AgentState.THINKING
        )
        assertThat(thinkingAction).isEqualTo(TriggerAction.PanicCancel)
    }

    @Test
    fun `quando estado for IDLE e usuario der toque curto no Volume Menos, ajusta volume normalmente`() {
        // Pressiona
        val downAction = interceptor.onKeyEvent(
            keyCode = KEYCODE_VOLUME_DOWN,
            isKeyDown = true,
            eventTimeMs = 1000L,
            currentState = AgentState.IDLE
        )
        assertThat(downAction).isEqualTo(TriggerAction.ConsumeQuietly)

        // Solta 200ms depois (< 500ms)
        val upAction = interceptor.onKeyEvent(
            keyCode = KEYCODE_VOLUME_DOWN,
            isKeyDown = false,
            eventTimeMs = 1200L,
            currentState = AgentState.IDLE
        )
        assertThat(upAction).isEqualTo(TriggerAction.AdjustVolumeDown)
    }

    @Test
    fun `quando estado for IDLE e usuario segurar Volume Menos por 900ms, dispara ActivateListening`() = testScope.runTest {
        var triggeredAction: TriggerAction? = null
        interceptor.onHoldTriggered = { triggeredAction = it }

        // Pressiona o botão
        val downAction = interceptor.onKeyEvent(
            keyCode = KEYCODE_VOLUME_DOWN,
            isKeyDown = true,
            eventTimeMs = 1000L,
            currentState = AgentState.IDLE
        )
        assertThat(downAction).isEqualTo(TriggerAction.ConsumeQuietly)

        // Avança 899ms (ainda não atingiu)
        advanceTimeBy(899L)
        assertThat(triggeredAction).isNull()

        // Avança mais 2ms (atingiu 901ms >= 900ms)
        advanceTimeBy(2L)
        assertThat(triggeredAction).isEqualTo(TriggerAction.ActivateListening)

        // Ao soltar depois do hold longo, consome silenciosamente sem baixar o volume
        val upAction = interceptor.onKeyEvent(
            keyCode = KEYCODE_VOLUME_DOWN,
            isKeyDown = false,
            eventTimeMs = 2100L,
            currentState = AgentState.IDLE
        )
        assertThat(upAction).isEqualTo(TriggerAction.ConsumeQuietly)
    }

    @Test
    fun `quando pressionar Volume Mais, ajusta volume para cima normalmente`() {
        val downAction = interceptor.onKeyEvent(
            keyCode = KEYCODE_VOLUME_UP,
            isKeyDown = true,
            eventTimeMs = 1000L,
            currentState = AgentState.IDLE
        )
        assertThat(downAction).isEqualTo(TriggerAction.AdjustVolumeUp)
    }
}
