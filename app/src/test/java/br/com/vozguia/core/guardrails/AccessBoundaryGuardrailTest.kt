package br.com.vozguia.core.guardrails

import br.com.vozguia.core.domain.ScreenSnapshot
import br.com.vozguia.core.domain.UserIntent
import br.com.vozguia.core.guardrails.impl.AccessBoundaryGuardrail
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class AccessBoundaryGuardrailTest {

    private lateinit var guardrail: AccessBoundaryGuardrail

    @Before
    fun setUp() {
        guardrail = AccessBoundaryGuardrail()
    }

    @Test
    fun `quando mensagem contiver link web com http ou https, bloqueia por seguranca anti-phishing`() = runTest {
        val dangerousLinks = listOf(
            "Clica aqui https://golpe.com/premio",
            "Acesse http://site-suspeito.org para ver a foto",
            "Veja em link.com/promocao agora",
            "Acesse www.site-falso.com.br urgente",
            "Entre no link bit.ly/promo-especial",
            "Veja no t.me/canal_sorteio"
        )

        for (linkText in dangerousLinks) {
            val intent = UserIntent.SendWhatsAppMessage(
                targetContact = "Gabriel",
                messageText = linkText
            )

            val verdict = guardrail.evaluate(intent, null)

            assertThat(verdict).isInstanceOf(GuardrailVerdict.Blocked::class.java)
            val blocked = verdict as GuardrailVerdict.Blocked
            assertThat(blocked.spokenMessage).contains("não é permitido ditar links da internet")
        }
    }

    @Test
    fun `quando mensagem for texto comum sem links, permite envio`() = runTest {
        val intent = UserIntent.SendWhatsAppMessage(
            targetContact = "Gabriel",
            messageText = "Oi filho, já tomei o café e estou bem."
        )

        val verdict = guardrail.evaluate(intent, null)
        assertThat(verdict).isEqualTo(GuardrailVerdict.Allowed)
    }

    @Test
    fun `quando app em primeiro plano for desconhecido e intencao for de interacao, bloqueia com mensagem orientativa`() = runTest {
        val screen = ScreenSnapshot(packageName = "com.untrusted.randomgame")
        val verdict = guardrail.evaluate(UserIntent.YouTubeSkipAd, screen)

        assertThat(verdict).isInstanceOf(GuardrailVerdict.Blocked::class.java)
        val blocked = verdict as GuardrailVerdict.Blocked
        assertThat(blocked.spokenMessage).contains("O Voz Guia só pode interagir com WhatsApp, Telefone e YouTube")
    }

    @Test
    fun `quando app for desconhecido mas intencao for consulta global de horas ou parar, permite`() = runTest {
        val screen = ScreenSnapshot(packageName = "com.untrusted.randomgame")

        val timeVerdict = guardrail.evaluate(UserIntent.TimeQuery, screen)
        assertThat(timeVerdict).isEqualTo(GuardrailVerdict.Allowed)

        val stopVerdict = guardrail.evaluate(UserIntent.StopOrCancel, screen)
        assertThat(stopVerdict).isEqualTo(GuardrailVerdict.Allowed)
    }

    @Test
    fun `quando tela atual for launcher inicial do Android, permite operacao`() = runTest {
        val launcherScreen = ScreenSnapshot(
            packageName = "com.google.android.apps.nexuslauncher"
        )

        val verdict = guardrail.evaluate(UserIntent.TimeQuery, launcherScreen)
        assertThat(verdict).isEqualTo(GuardrailVerdict.Allowed)
    }

    @Test
    fun `quando app for desconhecido mas intencao for EmergencySos ou acao global segura, permite execucao`() = runTest {
        val screen = ScreenSnapshot(packageName = "com.android.chrome")

        assertThat(guardrail.evaluate(UserIntent.EmergencySos, screen)).isEqualTo(GuardrailVerdict.Allowed)
        assertThat(guardrail.evaluate(UserIntent.RepeatLast, screen)).isEqualTo(GuardrailVerdict.Allowed)
        assertThat(guardrail.evaluate(UserIntent.ReadUnreadWhatsAppMessages, screen)).isEqualTo(GuardrailVerdict.Allowed)
        assertThat(guardrail.evaluate(UserIntent.PauseMedia, screen)).isEqualTo(GuardrailVerdict.Allowed)
        assertThat(guardrail.evaluate(UserIntent.YouTubeSearchAndPlay("jovem guarda"), screen)).isEqualTo(GuardrailVerdict.Allowed)
    }

    @Test
    fun `quando tela atual for o proprio pacote Voz Guia, permite leitura e navegacao`() = runTest {
        val packages = listOf("br.com.vozguia", "br.com.gabriel.vozguia")
        for (pkg in packages) {
            val screen = ScreenSnapshot(packageName = pkg)
            val verdict = guardrail.evaluate(UserIntent.YouTubeSkipAd, screen)
            assertThat(verdict).isEqualTo(GuardrailVerdict.Allowed)
        }
    }
}
