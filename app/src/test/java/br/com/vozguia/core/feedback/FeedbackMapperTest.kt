package br.com.vozguia.core.feedback

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FeedbackMapperTest {

    @Test
    fun `LISTENING_START deve mapear para pulso firme de 50ms e amplitude 180`() {
        val pattern = FeedbackMapper.getHapticPattern(FeedbackEvent.LISTENING_START)

        assertThat(pattern.timings).asList().containsExactly(0L, 50L).inOrder()
        assertThat(pattern.amplitudes).asList().containsExactly(0, 180).inOrder()
    }

    @Test
    fun `THINKING_START deve mapear para micro-clique de 25ms e amplitude 100`() {
        val pattern = FeedbackMapper.getHapticPattern(FeedbackEvent.THINKING_START)

        assertThat(pattern.timings).asList().containsExactly(0L, 25L).inOrder()
        assertThat(pattern.amplitudes).asList().containsExactly(0, 100).inOrder()
    }

    @Test
    fun `HEARTBEAT deve mapear para pulso de 15ms a cada ciclo`() {
        val pattern = FeedbackMapper.getHapticPattern(FeedbackEvent.HEARTBEAT)

        assertThat(pattern.timings).asList().containsExactly(0L, 15L).inOrder()
        assertThat(pattern.amplitudes).asList().containsExactly(0, 60).inOrder()
    }

    @Test
    fun `SUCCESS deve mapear para vibracao dupla 40ms pausa 60ms e pulso 60ms`() {
        val pattern = FeedbackMapper.getHapticPattern(FeedbackEvent.SUCCESS)

        assertThat(pattern.timings).asList().containsExactly(0L, 40L, 60L, 60L).inOrder()
        assertThat(pattern.amplitudes).asList().containsExactly(0, 200, 0, 200).inOrder()
    }

    @Test
    fun `CANCELLED deve mapear para pulso grave de 80ms`() {
        val pattern = FeedbackMapper.getHapticPattern(FeedbackEvent.CANCELLED)

        assertThat(pattern.timings).asList().containsExactly(0L, 80L).inOrder()
        assertThat(pattern.amplitudes).asList().containsExactly(0, 150).inOrder()
    }

    @Test
    fun `WAKEUP deve mapear para pulso sutil de despertar`() {
        val pattern = FeedbackMapper.getHapticPattern(FeedbackEvent.WAKEUP)

        assertThat(pattern.timings).asList().containsExactly(0L, 30L).inOrder()
        assertThat(pattern.amplitudes).asList().containsExactly(0, 120).inOrder()
    }
}
