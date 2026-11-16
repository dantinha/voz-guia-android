package br.com.vozguia.core.accessibility.parser

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import br.com.vozguia.core.domain.ScreenRect

/**
 * Interface de abstração sobre o AccessibilityNodeInfo do Android.
 * Permite testes unitários na JVM pura sem depender de instâncias reais do SO.
 */
interface AccessibleNode {
    val text: CharSequence?
    val contentDescription: CharSequence?
    val className: CharSequence?
    val packageName: CharSequence?
    val viewIdResourceName: String? get() = null
    val isVisibleToUser: Boolean
    val isClickable: Boolean
    val isPassword: Boolean
    val bounds: ScreenRect
    val childCount: Int
    fun getChild(index: Int): AccessibleNode?
}

/**
 * Adaptador para nós nativos do Android AccessibilityService.
 */
class AndroidAccessibilityNode(
    val nodeInfo: AccessibilityNodeInfo
) : AccessibleNode {
    override val text: CharSequence? get() = nodeInfo.text
    override val contentDescription: CharSequence? get() = nodeInfo.contentDescription
    override val className: CharSequence? get() = nodeInfo.className
    override val packageName: CharSequence? get() = nodeInfo.packageName
    override val viewIdResourceName: String? get() = nodeInfo.viewIdResourceName
    override val isVisibleToUser: Boolean get() = nodeInfo.isVisibleToUser
    override val isClickable: Boolean get() = nodeInfo.isClickable
    override val isPassword: Boolean get() = nodeInfo.isPassword

    override val bounds: ScreenRect get() {
        val rect = Rect()
        nodeInfo.getBoundsInScreen(rect)
        return ScreenRect(rect.left, rect.top, rect.right, rect.bottom)
    }

    override val childCount: Int get() = nodeInfo.childCount

    override fun getChild(index: Int): AccessibleNode? {
        val child = nodeInfo.getChild(index) ?: return null
        return AndroidAccessibilityNode(child)
    }
}
