package br.com.vozguia.core.guardrails.impl

import br.com.vozguia.core.domain.ScreenSnapshot
import br.com.vozguia.core.domain.SemanticElement
import br.com.vozguia.core.domain.UserIntent
import br.com.vozguia.core.guardrails.GuardrailVerdict
import br.com.vozguia.core.guardrails.VozGuiaGuardrail

/**
 * Filtro de nós sensíveis e proteção de credenciais.
 * Impede que campos de senha e dados sigilosos sejam lidos em voz alta ou clicados.
 * Prioridade: 80.
 */
class SensitiveNodeFilter : VozGuiaGuardrail {

    override val name: String = "SensitiveNodeFilter"
    override val priority: Int = 80

    private val sensitiveKeywords = setOf("senha", "password", "pin", "cvv", "código de segurança", "codigo de seguranca")

    override suspend fun evaluate(
        intent: UserIntent,
        currentScreen: ScreenSnapshot?
    ): GuardrailVerdict {
        // Consultas globais informativas e comandos de emergência/cancelamento NUNCA são bloqueados
        if (intent is UserIntent.EmergencySos ||
            intent is UserIntent.TimeQuery || intent is UserIntent.DateQuery ||
            intent is UserIntent.BatteryQuery || intent is UserIntent.StopOrCancel ||
            intent is UserIntent.DirectCall) {
            return GuardrailVerdict.Allowed
        }

        // Se a tela possui campos de senha e o usuário tenta um comando de interação em nós sensíveis
        if (currentScreen != null && currentScreen.hasPasswordFields) {
            val sensitiveNodeClicked = currentScreen.actionableElements.any { it.isPassword }
            if (sensitiveNodeClicked) {
                return GuardrailVerdict.Blocked(
                    reason = "Detecção de nó marcado como isPassword na tela ativa",
                    spokenMessage = "Por segurança, campos de senha não podem ser operados por comando de voz."
                )
            }
        }

        return GuardrailVerdict.Allowed
    }

    /**
     * Sanitiza a lista de elementos visíveis, removendo qualquer nó sigiloso
     * antes de sintetizar voz ou exibir informações.
     */
    fun sanitizeElements(elements: List<SemanticElement>): List<SemanticElement> {
        return elements.filterNot { element ->
            element.isPassword ||
                    sensitiveKeywords.any { kw ->
                        element.text?.contains(kw, ignoreCase = true) == true ||
                                element.contentDescription?.contains(kw, ignoreCase = true) == true
                    }
        }
    }
}
