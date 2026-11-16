package br.com.vozguia.core.accessibility.action

import android.view.accessibility.AccessibilityNodeInfo
import br.com.vozguia.core.domain.ScreenRect

/**
 * Interface para elementos clicáveis na árvore de acessibilidade.
 */
interface ClickableTarget {
    val bounds: ScreenRect
    fun performClick(): Boolean
}

/**
 * Adaptador para nós reais do Android AccessibilityNodeInfo.
 */
class AndroidClickableTarget(
    private val node: AccessibilityNodeInfo
) : ClickableTarget {
    override val bounds: ScreenRect get() {
        val rect = android.graphics.Rect()
        node.getBoundsInScreen(rect)
        return ScreenRect(rect.left, rect.top, rect.right, rect.bottom)
    }

    override fun performClick(): Boolean {
        return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }
}

/**
 * Resultado da tentativa de despacho de clique.
 */
sealed interface ActionDispatchResult {
    data object ClickedViaNode : ActionDispatchResult
    data object ClickedViaGesture : ActionDispatchResult
    data object Failed : ActionDispatchResult
}

/**
 * Despachante confiável de cliques programáticos.
 * Tenta primeiro o clique semântico no nó; se falhar, injeta gesto de toque de 80ms no centro do elemento.
 */
class ActionDispatcher(
    private val defaultTapDurationMs: Long = 80L
) {

    fun click(
        target: ClickableTarget,
        gestureFallback: (x: Int, y: Int, durationMs: Long) -> Boolean
    ): ActionDispatchResult {
        // Tentativa 1: Clique semântico direto no nó
        if (target.performClick()) {
            return ActionDispatchResult.ClickedViaNode
        }

        // Tentativa 2: Fallback por injeção de gesto no centro geométrico do nó
        val centerX = target.bounds.centerX
        val centerY = target.bounds.centerY

        val gestureSuccess = gestureFallback(centerX, centerY, defaultTapDurationMs)
        return if (gestureSuccess) {
            ActionDispatchResult.ClickedViaGesture
        } else {
            ActionDispatchResult.Failed
        }
    }
}
