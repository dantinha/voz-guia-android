package br.com.vozguia.core.guardrails

/**
 * Veredito resultante da avaliação de um ou mais guardrails.
 */
sealed interface GuardrailVerdict {

    /**
     * Ação totalmente permitida para execução imediata.
     */
    data object Allowed : GuardrailVerdict

    /**
     * Ação bloqueada por motivos de segurança, proteção financeira,
     * restrição de acesso ou esgotamento de quota.
     *
     * @param reason Mensagem técnica para auditoria e logs locais.
     * @param spokenMessage Mensagem falada humanizada, direta e segura para o usuário via TTS.
     */
    data class Blocked(
        val reason: String,
        val spokenMessage: String
    ) : GuardrailVerdict

    /**
     * Ação permitida exclusivamente após confirmação falada do usuário (2FA Vocal).
     *
     * @param confirmationPrompt Frase que o assistente deve perguntar em voz alta.
     * @param actionPayload Descrição ou executor da ação retida até confirmação.
     */
    data class RequiresVoiceConfirmation(
        val confirmationPrompt: String,
        val recipientName: String,
        val messageContent: String
    ) : GuardrailVerdict
}
