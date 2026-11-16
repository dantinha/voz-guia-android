package br.com.vozguia.core.guardrails

import br.com.vozguia.core.domain.ScreenSnapshot
import br.com.vozguia.core.domain.UserIntent

/**
 * Interface base para interceptadores de segurança, limites de uso,
 * controle de acesso e proteção de custos no Voz Guia.
 */
interface VozGuiaGuardrail {
    /**
     * Identificador do guardrail para auditoria e logs locais.
     */
    val name: String

    /**
     * Ordem de precedência no pipeline de avaliação.
     * Valores maiores são executados primeiro (Ex: FinancialShield = 100).
     */
    val priority: Int

    /**
     * Avalia se a intenção solicitada e o contexto atual da tela respeitam os limites operacionais.
     *
     * @param intent Intenção extraída da fala ou gatilho do usuário.
     * @param currentScreen Snapshot da tela visível no momento (se disponível).
     * @return GuardrailVerdict indicando Allowed, Blocked ou RequiresVoiceConfirmation.
     */
    suspend fun evaluate(
        intent: UserIntent,
        currentScreen: ScreenSnapshot?
    ): GuardrailVerdict
}
