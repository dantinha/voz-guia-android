package br.com.vozguia.core.accessibility.touchshield

/**
 * Classificação do tipo de toque interceptado pelo Escudo de Tela.
 */
sealed interface TouchClassification {
    /**
     * Toque acidental nas bordas curvas (Motorola Edge 50 Neo); absorvido sem ação.
     */
    data object AccidentalEdgeTouch : TouchClassification

    /**
     * Gesto amplo de palma da mão (ou 3+ dedos por 500ms); aciona a escuta do assistente.
     */
    data object PalmGesture : TouchClassification

    /**
     * Toque múltiplo detectado aguardando atingir o limiar de 500ms de pressão contínua.
     */
    data object AbsorbedPendingHold : TouchClassification

    /**
     * Escudo desativado ou pausado pelo cuidador; toque repassado ao app subjacente.
     */
    data object PassThrough : TouchClassification
}

/**
 * Classificador de toques na tela para proteção contra esbarrões nas bordas curvas
 * e suporte ao botão gigante de palma da mão.
 */
class TouchClassifier(
    private val screenWidth: Int,
    private val screenHeight: Int,
    private val edgeMarginPx: Int = 60,
    private val palmPointerCountThreshold: Int = 3,
    private val palmHoldDurationMs: Long = 500L,
    private val clockProvider: () -> Long = { System.currentTimeMillis() }
) {
    private var pausedUntilTimestampMs: Long = 0L

    fun pauseShield(durationMs: Long) {
        pausedUntilTimestampMs = clockProvider() + durationMs
    }

    fun resumeShield() {
        pausedUntilTimestampMs = 0L
    }

    val isPaused: Boolean get() = clockProvider() < pausedUntilTimestampMs

    fun classify(
        pointerCount: Int,
        x: Float,
        y: Float,
        holdDurationMs: Long
    ): TouchClassification {
        if (isPaused) {
            return TouchClassification.PassThrough
        }

        // Gesto amplo de palma (3 ou mais dedos simultâneos na tela)
        if (pointerCount >= palmPointerCountThreshold) {
            return if (holdDurationMs >= palmHoldDurationMs) {
                TouchClassification.PalmGesture
            } else {
                TouchClassification.AbsorbedPendingHold
            }
        }

        // Toque único acidental nas bordas curvas laterais
        val isLeftEdge = x <= edgeMarginPx
        val isRightEdge = x >= (screenWidth - edgeMarginPx)

        if (isLeftEdge || isRightEdge) {
            return TouchClassification.AccidentalEdgeTouch
        }

        return TouchClassification.PassThrough
    }
}
