package br.com.vozguia.core.guardrails

import br.com.vozguia.core.domain.ScreenSnapshot
import br.com.vozguia.core.domain.UserIntent

/**
 * Orquestrador do pipeline de guardrails do Voz Guia.
 * Executa os guardrails ordenados por prioridade e interrompe a avaliação no primeiro bloqueio ou confirmação exigida.
 */
class GuardrailPipeline(
    guardrails: List<VozGuiaGuardrail>
) {
    private val orderedGuardrails = guardrails.sortedByDescending { it.priority }

    suspend fun evaluate(
        intent: UserIntent,
        currentScreen: ScreenSnapshot?
    ): GuardrailVerdict {
        for (guardrail in orderedGuardrails) {
            val verdict = guardrail.evaluate(intent, currentScreen)
            if (verdict !is GuardrailVerdict.Allowed) {
                return verdict
            }
        }
        return GuardrailVerdict.Allowed
    }
}
