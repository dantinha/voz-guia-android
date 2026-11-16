package br.com.vozguia.core.voice.dialogue

import br.com.vozguia.core.domain.AgentState
import br.com.vozguia.core.domain.UserIntent

/**
 * Resultado do processamento de diálogo conversacional.
 */
sealed interface DialogueResult {
    data class Speak(val text: String, val nextState: AgentState = AgentState.IDLE) : DialogueResult
    data object ActionExecuted : DialogueResult
    data object ActionCancelled : DialogueResult
    data object Ignored : DialogueResult
}

/**
 * Gerenciador de Estado de Diálogo e Gerador de Respostas Humanizadas.
 * Aplica as regras de negócio:
 * - RN-2.01: Limite máximo de 2 opções cognitivas para não cansar o usuário.
 * - RN-2.02: Fallback offline transparente e acolhedor sem erros técnicos.
 */
class DialogueStateManager {

    var currentState: AgentState = AgentState.IDLE
        private set

    var pendingConfirmationPrompt: String? = null
        private set

    private var pendingConfirmAction: (() -> Unit)? = null

    fun setPendingConfirmation(prompt: String, onConfirm: () -> Unit) {
        pendingConfirmationPrompt = prompt
        pendingConfirmAction = onConfirm
        currentState = AgentState.AWAITING_CONFIRMATION
    }

    fun handleIntent(intent: UserIntent): DialogueResult {
        if (currentState == AgentState.AWAITING_CONFIRMATION) {
            return when (intent) {
                is UserIntent.ConfirmYes -> {
                    val action = pendingConfirmAction
                    clearPendingConfirmation()
                    action?.invoke()
                    currentState = AgentState.IDLE
                    DialogueResult.ActionExecuted
                }

                is UserIntent.ConfirmNo, is UserIntent.StopOrCancel -> {
                    clearPendingConfirmation()
                    currentState = AgentState.IDLE
                    DialogueResult.ActionCancelled
                }

                else -> DialogueResult.Ignored
            }
        }

        return DialogueResult.Ignored
    }

    fun clearPendingConfirmation() {
        pendingConfirmationPrompt = null
        pendingConfirmAction = null
    }

    /**
     * Resumo cognitivo enxuto de mensagens não lidas conforme RN-2.01.
     */
    fun formatUnreadSummary(senders: List<String>): String {
        if (senders.isEmpty()) {
            return "Você não tem nenhuma mensagem nova no momento."
        }

        if (senders.size == 1) {
            return "Você tem mensagens de ${senders[0]}. Quer que eu leia agora?"
        }

        if (senders.size == 2) {
            return "Você tem mensagens de ${senders[0]} e ${senders[1]}. Quer ouvir ${senders[0]} primeiro?"
        }

        val first = senders[0]
        val second = senders[1]
        val remaining = senders.size - 2
        return "Você tem mensagens de $first e $second, além de outras $remaining conversas. Quer ouvir o $first primeiro?"
    }

    /**
     * Mensagem de contingência offline conforme RN-2.02.
     */
    fun getOfflineFallbackMessage(): String {
        return "Estou sem internet agora para ver a imagem, mas consigo ler as mensagens de texto e fazer ligações normalmente."
    }
}
