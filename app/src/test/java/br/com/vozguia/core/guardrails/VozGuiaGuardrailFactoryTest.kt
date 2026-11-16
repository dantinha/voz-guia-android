package br.com.vozguia.core.guardrails

import br.com.vozguia.core.data.repository.QuotaRepository
import br.com.vozguia.core.domain.ScreenSnapshot
import br.com.vozguia.core.domain.UserIntent
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

class VozGuiaGuardrailFactoryTest {

    private val dummyQuotaRepo = object : QuotaRepository {
        override suspend fun getTodayCloudCallCount(): Int = 0
        override suspend fun getLastCloudCallTimestamp(): Long = 0L
        override suspend fun recordCloudCall(timestampMs: Long) {}
        override suspend fun resetDailyCount() {}
    }

    @Test
    fun `pipeline padrao de producao bloqueia apps bancarios e exige 2FA no WhatsApp`() = runTest {
        val pipeline = VozGuiaGuardrailFactory.createDefaultPipeline(dummyQuotaRepo)

        val bankVerdict = pipeline.evaluate(
            intent = UserIntent.TimeQuery,
            currentScreen = ScreenSnapshot(packageName = "com.nu.production")
        )
        assertThat(bankVerdict).isInstanceOf(GuardrailVerdict.Blocked::class.java)

        val waVerdict = pipeline.evaluate(
            intent = UserIntent.SendWhatsAppMessage("Gabriel", "Oi filho"),
            currentScreen = ScreenSnapshot(packageName = "com.whatsapp")
        )
        assertThat(waVerdict).isInstanceOf(GuardrailVerdict.RequiresVoiceConfirmation::class.java)
    }

    @Test
    fun `pipeline padrao nunca bloqueia EmergencySos mesmo com banco aberto`() = runTest {
        val pipeline = VozGuiaGuardrailFactory.createDefaultPipeline(dummyQuotaRepo)

        val sosVerdict = pipeline.evaluate(
            intent = UserIntent.EmergencySos,
            currentScreen = ScreenSnapshot(packageName = "com.nu.production")
        )
        assertThat(sosVerdict).isEqualTo(GuardrailVerdict.Allowed)
    }
}
