package br.com.vozguia.core.accessibility.action

import br.com.vozguia.core.domain.ScreenRect
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

class ActionDispatcherTest {

    private lateinit var dispatcher: ActionDispatcher

    @Before
    fun setUp() {
        dispatcher = ActionDispatcher()
    }

    @Test
    fun `quando performAction retornar true, conclui com ClickedViaNode`() {
        val target = FakeActionableNode(
            bounds = ScreenRect(100, 200, 300, 400),
            actionResult = true
        )

        var gestureDispatched = false
        val result = dispatcher.click(
            target = target,
            gestureFallback = { _, _, _ ->
                gestureDispatched = true
                true
            }
        )

        assertThat(result).isEqualTo(ActionDispatchResult.ClickedViaNode)
        assertThat(gestureDispatched).isFalse()
    }

    @Test
    fun `quando performAction retornar false, aciona gesto nas coordenadas centrais (80ms)`() {
        val target = FakeActionableNode(
            bounds = ScreenRect(100, 200, 300, 400),
            actionResult = false
        )

        var tappedX = 0
        var tappedY = 0
        var tappedDuration = 0L

        val result = dispatcher.click(
            target = target,
            gestureFallback = { x, y, durationMs ->
                tappedX = x
                tappedY = y
                tappedDuration = durationMs
                true
            }
        )

        assertThat(result).isEqualTo(ActionDispatchResult.ClickedViaGesture)
        // Centro de left=100, right=300 é 200
        assertThat(tappedX).isEqualTo(200)
        // Centro de top=200, bottom=400 é 300
        assertThat(tappedY).isEqualTo(300)
        assertThat(tappedDuration).isEqualTo(80L)
    }

    @Test
    fun `quando performAction e gesto falharem, retorna Failed`() {
        val target = FakeActionableNode(
            bounds = ScreenRect(0, 0, 100, 100),
            actionResult = false
        )

        val result = dispatcher.click(
            target = target,
            gestureFallback = { _, _, _ -> false }
        )

        assertThat(result).isEqualTo(ActionDispatchResult.Failed)
    }

    private class FakeActionableNode(
        override val bounds: ScreenRect,
        private val actionResult: Boolean
    ) : ClickableTarget {
        override fun performClick(): Boolean = actionResult
    }
}
