package br.com.vozguia.core.guardrails

import br.com.vozguia.core.domain.ScreenSnapshot
import br.com.vozguia.core.domain.UserIntent
import br.com.vozguia.core.guardrails.impl.FinancialShieldGuardrail
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class FinancialShieldGuardrailTest {

    private lateinit var guardrail: FinancialShieldGuardrail

    @Before
    fun setUp() {
        guardrail = FinancialShieldGuardrail()
    }

    @Test
    fun `quando app bancario estiver em primeiro plano, bloqueia com mensagem falada preventiva`() = runTest {
        val bankingApps = listOf(
            "com.nu.production",
            "br.com.bb",
            "com.itau",
            "br.com.bradesco",
            "br.com.santander.usuario",
            "br.gov.caixa.tem",
            "br.gov.caixa.fgts",
            "br.com.intermedium",
            "com.c6bank.app",
            "br.com.bancopan",
            "br.com.neon",
            "br.com.uol.ps",
            "com.mercadopago.wallet",
            "com.picpay",
            "com.google.android.apps.walletnfcrel",
            "br.com.xp.wallet",
            "br.com.rico",
            "com.btg.pactual.banking",
            "br.com.clear",
            "br.com.sicoobnet",
            "br.com.sicredi.mobile",
            "br.com.safra.bancosafra",
            "br.gov.meugovbr"
        )

        for (pkg in bankingApps) {
            val screen = ScreenSnapshot(packageName = pkg)
            val verdict = guardrail.evaluate(UserIntent.TimeQuery, screen)

            assertThat(verdict).isInstanceOf(GuardrailVerdict.Blocked::class.java)
            val blocked = verdict as GuardrailVerdict.Blocked
            assertThat(blocked.spokenMessage).contains("operações bancárias e transferências não podem ser feitas por voz")
            assertThat(blocked.reason).contains(pkg)
        }
    }

    @Test
    fun `quando app de mensageria ou youtube estiver aberto, permite execucao normalmente`() = runTest {
        val safePackages = listOf("com.whatsapp", "com.google.android.youtube", "br.com.vozguia")

        for (pkg in safePackages) {
            val screen = ScreenSnapshot(packageName = pkg)
            val verdict = guardrail.evaluate(UserIntent.TimeQuery, screen)

            assertThat(verdict).isEqualTo(GuardrailVerdict.Allowed)
        }
    }

    @Test
    fun `quando tela atual for nula, permite execucao`() = runTest {
        val verdict = guardrail.evaluate(UserIntent.BatteryQuery, null)
        assertThat(verdict).isEqualTo(GuardrailVerdict.Allowed)
    }

    @Test
    fun `quando app bancario estiver em primeiro plano mas intencao for EmergencySos, nunca bloqueia socorro`() = runTest {
        val bankScreen = ScreenSnapshot(packageName = "com.nu.production")
        val verdict = guardrail.evaluate(UserIntent.EmergencySos, bankScreen)
        assertThat(verdict).isEqualTo(GuardrailVerdict.Allowed)
    }
}
