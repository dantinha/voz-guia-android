package br.com.vozguia.core.guardrails

import br.com.vozguia.core.data.repository.QuotaRepository
import br.com.vozguia.core.domain.ScreenSnapshot
import br.com.vozguia.core.domain.UserIntent
import br.com.vozguia.core.guardrails.impl.AccessBoundaryGuardrail
import br.com.vozguia.core.guardrails.impl.CloudCostGuardrail
import br.com.vozguia.core.guardrails.impl.DataPrivacySanitizer
import br.com.vozguia.core.guardrails.impl.FinancialShieldGuardrail
import br.com.vozguia.core.guardrails.impl.SensitiveNodeFilter
import br.com.vozguia.core.guardrails.impl.Voice2FAGuardrail
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class GuardrailPipelineTest {

    private lateinit var pipeline: GuardrailPipeline
    private lateinit var fakeQuotaRepository: FakeQuotaRepository

    @Before
    fun setUp() {
        fakeQuotaRepository = FakeQuotaRepository()

        val guardrails: List<VozGuiaGuardrail> = listOf(
            FinancialShieldGuardrail(),     // Priority 100
            AccessBoundaryGuardrail(),      // Priority 90
            SensitiveNodeFilter(),          // Priority 80
            Voice2FAGuardrail(),            // Priority 70
            DataPrivacySanitizer(),         // Priority 60
            CloudCostGuardrail(fakeQuotaRepository) // Priority 50
        )

        pipeline = GuardrailPipeline(guardrails)
    }

    @Test
    fun `FinancialShield tem prioridade maxima e bloqueia antes do Voice2FA`() = runTest {
        // Usuário tenta enviar mensagem enquanto o app do Nubank está aberto
        val intent = UserIntent.SendWhatsAppMessage(
            targetContact = "Gabriel",
            messageText = "Transferi o dinheiro"
        )
        val bankingScreen = ScreenSnapshot(packageName = "com.nu.production")

        val verdict = pipeline.evaluate(intent, bankingScreen)

        // Deve ser bloqueado pelo FinancialShield (prioridade 100), e NÃO pedir confirmação do Voice2FA
        assertThat(verdict).isInstanceOf(GuardrailVerdict.Blocked::class.java)
        val blocked = verdict as GuardrailVerdict.Blocked
        assertThat(blocked.spokenMessage).contains("operações bancárias e transferências não podem ser feitas por voz")
    }

    @Test
    fun `em tela normal de WhatsApp, mensagem valida passa para Voice2FA`() = runTest {
        val intent = UserIntent.SendWhatsAppMessage(
            targetContact = "Gabriel",
            messageText = "Já almocei, tudo tranquilo por aqui."
        )
        val whatsappScreen = ScreenSnapshot(packageName = "com.whatsapp")

        val verdict = pipeline.evaluate(intent, whatsappScreen)

        assertThat(verdict).isInstanceOf(GuardrailVerdict.RequiresVoiceConfirmation::class.java)
        val confirmation = verdict as GuardrailVerdict.RequiresVoiceConfirmation
        assertThat(confirmation.recipientName).isEqualTo("Gabriel")
        assertThat(confirmation.messageContent).isEqualTo("Já almocei, tudo tranquilo por aqui.")
    }

    @Test
    fun `comando Fast-Path local de horas em tela comum e aprovado diretamente`() = runTest {
        val whatsappScreen = ScreenSnapshot(packageName = "com.whatsapp")
        val verdict = pipeline.evaluate(UserIntent.TimeQuery, whatsappScreen)

        assertThat(verdict).isEqualTo(GuardrailVerdict.Allowed)
    }

    private class FakeQuotaRepository : QuotaRepository {
        override suspend fun getTodayCloudCallCount(): Int = 0
        override suspend fun getLastCloudCallTimestamp(): Long = 0L
        override suspend fun recordCloudCall(timestampMs: Long) {}
        override suspend fun resetDailyCount() {}
    }
}
