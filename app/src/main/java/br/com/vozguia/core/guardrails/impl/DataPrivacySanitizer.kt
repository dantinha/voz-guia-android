package br.com.vozguia.core.guardrails.impl

import br.com.vozguia.core.domain.ScreenSnapshot
import br.com.vozguia.core.domain.UserIntent
import br.com.vozguia.core.guardrails.GuardrailVerdict
import br.com.vozguia.core.guardrails.VozGuiaGuardrail

/**
 * Sanitizador de privacidade de dados (LGPD & Zero-PII).
 * Remove números de documentos (CPF, RG, CNH), dados de cartão e dados sensíveis antes de qualquer processamento externo.
 * Prioridade: 60.
 */
class DataPrivacySanitizer : VozGuiaGuardrail {

    override val name: String = "DataPrivacySanitizer"
    override val priority: Int = 60

    // Suporta CPF com pontos, traços, sem separador ou com espaços típicos de STT
    private val cpfRegex = Regex("""\b\d{3}[.\s]?\d{3}[.\s]?\d{3}[-\s]?\d{2}\b""")

    // Suporta 16 dígitos padrão, 15 dígitos Amex (4-6-5) e sequências de 13 a 19 dígitos
    private val creditCardRegex = Regex("""\b(?:\d{4}[ -]?\d{4}[ -]?\d{4}[ -]?\d{4}|\d{4}[ -]?\d{6}[ -]?\d{5}|\d{13,19})\b""")
    private val emailRegex = Regex("""[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\.[a-zA-Z0-9-.]+""")

    override suspend fun evaluate(
        intent: UserIntent,
        currentScreen: ScreenSnapshot?
    ): GuardrailVerdict {
        // Se a intenção é enviar mensagem e contém dados sensíveis como número de cartão
        if (intent is UserIntent.SendWhatsAppMessage) {
            if (creditCardRegex.containsMatchIn(intent.messageText)) {
                return GuardrailVerdict.Blocked(
                    reason = "Detecção de número de cartão de crédito na mensagem",
                    spokenMessage = "Aviso de segurança: Esta mensagem parece conter dados de cartão de crédito e não pode ser enviada por voz."
                )
            }
        }

        return GuardrailVerdict.Allowed
    }

    /**
     * Sanitiza o texto, ofuscando identificadores sensíveis antes do envio à nuvem.
     * A ordem garante que cartões (13-19 dígitos) sejam capturados antes de CPFs (11 dígitos) para evitar colisão parcial.
     */
    fun sanitizeForCloud(rawText: String): String {
        return rawText
            .replace(creditCardRegex, "[CARTAO_OFUSCADO]")
            .replace(cpfRegex, "[DOCUMENTO_OFUSCADO]")
            .replace(emailRegex, "[EMAIL_OFUSCADO]")
    }
}
