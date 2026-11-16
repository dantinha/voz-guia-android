package br.com.vozguia.core.guardrails

import br.com.vozguia.core.domain.UserIntent
import br.com.vozguia.core.guardrails.impl.Voice2FAGuardrail
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class Voice2FAGuardrailTest {

    private lateinit var guardrail: Voice2FAGuardrail

    @Before
    fun setUp() {
        guardrail = Voice2FAGuardrail()
    }

    @Test
    fun `quando intencao for enviar mensagem no WhatsApp, exige confirmacao vocal explicita`() = runTest {
        val intent = UserIntent.SendWhatsAppMessage(
            targetContact = "Gabriel",
            messageText = "Já tomei o remédio da manhã"
        )

        val verdict = guardrail.evaluate(intent, null)

        assertThat(verdict).isInstanceOf(GuardrailVerdict.RequiresVoiceConfirmation::class.java)
        val confirmation = verdict as GuardrailVerdict.RequiresVoiceConfirmation
        assertThat(confirmation.recipientName).isEqualTo("Gabriel")
        assertThat(confirmation.messageContent).isEqualTo("Já tomei o remédio da manhã")
        assertThat(confirmation.confirmationPrompt).contains("Você disse: \"Já tomei o remédio da manhã\" para Gabriel. Posso enviar?")
    }

    @Test
    fun `quando destinatario for em branco, bloqueia com aviso explicativo`() = runTest {
        val intent = UserIntent.SendWhatsAppMessage(
            targetContact = "   ",
            messageText = "Já tomei o remédio"
        )

        val verdict = guardrail.evaluate(intent, null)

        assertThat(verdict).isInstanceOf(GuardrailVerdict.Blocked::class.java)
        val blocked = verdict as GuardrailVerdict.Blocked
        assertThat(blocked.spokenMessage).contains("Não entendi para quem você quer enviar")
    }

    @Test
    fun `quando texto da mensagem for em branco, bloqueia com aviso explicativo`() = runTest {
        val intent = UserIntent.SendWhatsAppMessage(
            targetContact = "Gabriel",
            messageText = "   "
        )

        val verdict = guardrail.evaluate(intent, null)

        assertThat(verdict).isInstanceOf(GuardrailVerdict.Blocked::class.java)
        val blocked = verdict as GuardrailVerdict.Blocked
        assertThat(blocked.spokenMessage).contains("A mensagem está vazia")
    }

    @Test
    fun `quando intencao for consulta de horas ou ligacao direta, permite sem 2FA`() = runTest {
        val timeVerdict = guardrail.evaluate(UserIntent.TimeQuery, null)
        assertThat(timeVerdict).isEqualTo(GuardrailVerdict.Allowed)

        val callVerdict = guardrail.evaluate(
            UserIntent.DirectCall(targetName = "Gabriel"),
            null
        )
        assertThat(callVerdict).isEqualTo(GuardrailVerdict.Allowed)
    }

    @Test
    fun `quando intencao for gravar audio no WhatsApp, exige destinatario valido e libera gravacao sem 2FA de texto`() = runTest {
        val validVerdict = guardrail.evaluate(UserIntent.SendWhatsAppVoiceNote(targetContact = "Gabriel"), null)
        assertThat(validVerdict).isEqualTo(GuardrailVerdict.Allowed)

        val blankVerdict = guardrail.evaluate(UserIntent.SendWhatsAppVoiceNote(targetContact = " "), null)
        assertThat(blankVerdict).isInstanceOf(GuardrailVerdict.Blocked::class.java)
    }
}
