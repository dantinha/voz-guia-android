package br.com.vozguia.core.guardrails.impl

import br.com.vozguia.core.domain.ScreenSnapshot
import br.com.vozguia.core.domain.UserIntent
import br.com.vozguia.core.guardrails.GuardrailVerdict
import br.com.vozguia.core.guardrails.VozGuiaGuardrail

/**
 * Garante que qualquer mensagem externa (WhatsApp) exija confirmação falada explícita (2FA Vocal)
 * antes de ser despachada.
 * Prioridade: 70.
 */
class Voice2FAGuardrail : VozGuiaGuardrail {

    override val name: String = "Voice2FAGuardrail"
    override val priority: Int = 70

    override suspend fun evaluate(
        intent: UserIntent,
        currentScreen: ScreenSnapshot?
    ): GuardrailVerdict {
        if (intent is UserIntent.SendWhatsAppMessage) {
            val formattedRecipient = intent.targetContact.trim()
            val formattedText = intent.messageText.trim()

            if (formattedRecipient.isBlank()) {
                return GuardrailVerdict.Blocked(
                    reason = "Destinatário em branco",
                    spokenMessage = "Não entendi para quem você quer enviar a mensagem. Para quem devo enviar?"
                )
            }

            if (formattedText.isBlank()) {
                return GuardrailVerdict.Blocked(
                    reason = "Mensagem vazia",
                    spokenMessage = "A mensagem está vazia. O que você gostaria de dizer?"
                )
            }

            val prompt = "Você disse: \"$formattedText\" para $formattedRecipient. Posso enviar?"

            return GuardrailVerdict.RequiresVoiceConfirmation(
                confirmationPrompt = prompt,
                recipientName = formattedRecipient,
                messageContent = formattedText
            )
        }

        return GuardrailVerdict.Allowed
    }
}
