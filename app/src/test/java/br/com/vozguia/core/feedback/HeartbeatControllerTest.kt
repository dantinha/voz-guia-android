package br.com.vozguia.core.feedback

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HeartbeatControllerTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var controller: HeartbeatController
    private var heartbeatCount = 0

    @Before
    fun setUp() {
        heartbeatCount = 0
        controller = HeartbeatController(
            coroutineScope = testScope,
            intervalMs = 1000L,
            initialDelayMs = 1000L,
            onHeartbeat = { heartbeatCount++ }
        )
    }

    @Test
    fun `quando iniciar heartbeat, so comeca a pulsar apos 1 segundo`() = testScope.runTest {
        controller.start()

        // Antes de 1s (nenhum pulso)
        advanceTimeBy(999L)
        assertThat(heartbeatCount).isEqualTo(0)

        // Ao completar 1s (primeiro pulso)
        advanceTimeBy(2L)
        assertThat(heartbeatCount).isEqualTo(1)

        // Ao completar 2s (segundo pulso)
        advanceTimeBy(1000L)
        assertThat(heartbeatCount).isEqualTo(2)

        // Ao parar, nenhum pulso adicional deve ocorrer
        controller.stop()
        advanceTimeBy(2000L)
        assertThat(heartbeatCount).isEqualTo(2)
    }

    @Test
    fun `iniciar duas vezes consecutivas nao duplica pulsos`() = testScope.runTest {
        controller.start()
        controller.start()

        advanceTimeBy(1001L)
        assertThat(heartbeatCount).isEqualTo(1)

        controller.stop()
    }
}
