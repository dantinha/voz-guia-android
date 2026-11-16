package br.com.vozguia.core.accessibility.touchshield

import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

class TouchClassifierTest {

    private lateinit var classifier: TouchClassifier

    private val screenWidth = 1080
    private val screenHeight = 2400

    @Before
    fun setUp() {
        classifier = TouchClassifier(
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            edgeMarginPx = 60, // 60px de margem das bordas curvas
            palmPointerCountThreshold = 3,
            palmHoldDurationMs = 500L
        )
    }

    @Test
    fun `toque unico nas bordas curvas laterais deve ser classificado como AccidentalEdgeTouch`() {
        // Toque na borda esquerda (X = 30px)
        val leftEdgeTouch = classifier.classify(
            pointerCount = 1,
            x = 30f,
            y = 1200f,
            holdDurationMs = 100L
        )
        assertThat(leftEdgeTouch).isEqualTo(TouchClassification.AccidentalEdgeTouch)

        // Toque na borda direita (X = 1050px)
        val rightEdgeTouch = classifier.classify(
            pointerCount = 1,
            x = 1050f,
            y = 1200f,
            holdDurationMs = 100L
        )
        assertThat(rightEdgeTouch).isEqualTo(TouchClassification.AccidentalEdgeTouch)
    }

    @Test
    fun `palma ou 3 ou mais dedos por mais de 500ms deve ser classificado como PalmGesture`() {
        val palmTouch = classifier.classify(
            pointerCount = 3,
            x = 540f,
            y = 1200f,
            holdDurationMs = 520L
        )

        assertThat(palmTouch).isEqualTo(TouchClassification.PalmGesture)
    }

    @Test
    fun `toque de 3 dedos com duracao menor que 500ms ainda nao dispara palma`() {
        val quickMultiTouch = classifier.classify(
            pointerCount = 3,
            x = 540f,
            y = 1200f,
            holdDurationMs = 200L
        )

        assertThat(quickMultiTouch).isEqualTo(TouchClassification.AbsorbedPendingHold)
    }

    @Test
    fun `quando escudo estiver pausado pelo cuidador, repassa toque normalmente`() {
        classifier.pauseShield(durationMs = 15 * 60 * 1000L)

        val touch = classifier.classify(
            pointerCount = 1,
            x = 30f,
            y = 1200f,
            holdDurationMs = 100L
        )

        assertThat(touch).isEqualTo(TouchClassification.PassThrough)
    }
}
