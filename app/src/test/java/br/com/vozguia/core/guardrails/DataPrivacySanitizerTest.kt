package br.com.vozguia.core.guardrails

import br.com.vozguia.core.domain.UserIntent
import br.com.vozguia.core.guardrails.impl.DataPrivacySanitizer
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class DataPrivacySanitizerTest {

    private lateinit var sanitizer: DataPrivacySanitizer

    @Before
    fun setUp() {
        sanitizer = DataPrivacySanitizer()
    }

    @Test
    fun `sanitizeForCloud deve ofuscar CPF com espacos emitido por STT e cartao Amex de 15 digitos`() {
        val prompt = "Meu CPF falado é 123 456 789 00 e o cartão Amex é 3412 345678 12345."
        val sanitized = sanitizer.sanitizeForCloud(prompt)

        assertThat(sanitized).doesNotContain("123 456 789 00")
        assertThat(sanitized).doesNotContain("3412 345678 12345")
        assertThat(sanitized).contains("[DOCUMENTO_OFUSCADO]")
        assertThat(sanitized).contains("[CARTAO_OFUSCADO]")
    }

    @Test
    fun `sanitizeForCloud deve ofuscar CPF, cartao de credito e email antes do envio a nuvem`() {
        val rawPrompt = "O CPF do Gabriel é 123.456.789-00, o email é teste@email.com e o cartão é 4532 1122 3344 5566."

        val sanitized = sanitizer.sanitizeForCloud(rawPrompt)

        assertThat(sanitized).doesNotContain("123.456.789-00")
        assertThat(sanitized).doesNotContain("teste@email.com")
        assertThat(sanitized).doesNotContain("4532 1122 3344 5566")
        assertThat(sanitized).contains("[DOCUMENTO_OFUSCADO]")
        assertThat(sanitized).contains("[EMAIL_OFUSCADO]")
        assertThat(sanitized).contains("[CARTAO_OFUSCADO]")
    }

    @Test
    fun `quando mensagem contiver cartao Amex com 15 digitos, bloqueia envio no WhatsApp preventivamente`() = runTest {
        val dangerousIntent = UserIntent.SendWhatsAppMessage(
            targetContact = "Desconhecido",
            messageText = "Meu cartão Amex é 3782 822463 10005"
        )

        val verdict = sanitizer.evaluate(dangerousIntent, null)

        assertThat(verdict).isInstanceOf(GuardrailVerdict.Blocked::class.java)
        val blocked = verdict as GuardrailVerdict.Blocked
        assertThat(blocked.spokenMessage).contains("dados de cartão de crédito e não pode ser enviada por voz")
    }

    @Test
    fun `quando mensagem contiver cartao de credito, bloqueia envio no WhatsApp preventivamente`() = runTest {
        val dangerousIntent = UserIntent.SendWhatsAppMessage(
            targetContact = "Desconhecido",
            messageText = "Meu cartão é 5502 0102 0304 0506"
        )

        val verdict = sanitizer.evaluate(dangerousIntent, null)

        assertThat(verdict).isInstanceOf(GuardrailVerdict.Blocked::class.java)
        val blocked = verdict as GuardrailVerdict.Blocked
        assertThat(blocked.spokenMessage).contains("dados de cartão de crédito e não pode ser enviada por voz")
    }
}
