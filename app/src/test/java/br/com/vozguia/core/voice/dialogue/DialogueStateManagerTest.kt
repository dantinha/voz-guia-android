package br.com.vozguia.core.voice.dialogue

import br.com.vozguia.core.domain.AgentState
import br.com.vozguia.core.domain.UserIntent
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

class DialogueStateManagerTest {

    private lateinit var dialogueManager: DialogueStateManager

    @Before
    fun setUp() {
        dialogueManager = DialogueStateManager()
    }

    @Test
    fun `formatUnreadSummary deve respeitar limite de 2 opcoes cognitivas (RN-2_01)`() {
        val senders = listOf("Gabriel", "Maria", "João", "Clube de Pesca", "Notícias Locais")

        val summary = dialogueManager.formatUnreadSummary(senders)

        // Deve citar no máximo 2 nomes e resumir o restante
        assertThat(summary).contains("Gabriel e Maria")
        assertThat(summary).contains("além de outras 3 conversas")
        assertThat(summary).contains("Quer ouvir o Gabriel primeiro?")
        assertThat(summary).doesNotContain("Clube de Pesca")
    }

    @Test
    fun `formatUnreadSummary com apenas 1 remetente deve gerar frase simples e direta`() {
        val senders = listOf("Gabriel")

        val summary = dialogueManager.formatUnreadSummary(senders)

        assertThat(summary).isEqualTo("Você tem mensagens de Gabriel. Quer que eu leia agora?")
    }

    @Test
    fun `offlineFallbackMessage deve retornar frase acolhedora sem jargao tecnico (RN-2_02)`() {
        val message = dialogueManager.getOfflineFallbackMessage()

        assertThat(message).contains("Estou sem internet agora para ver a imagem")
        assertThat(message).contains("consigo ler as mensagens de texto e fazer ligações normalmente")
        assertThat(message).doesNotContain("erro 404")
        assertThat(message).doesNotContain("timeout")
    }

    @Test
    fun `quando em AWAITING_CONFIRMATION e receber ConfirmYes, executa acao pendente`() {
        var actionExecuted = false
        dialogueManager.setPendingConfirmation(
            prompt = "Você disse: 'Oi' para Gabriel. Posso enviar?",
            onConfirm = { actionExecuted = true }
        )

        assertThat(dialogueManager.currentState).isEqualTo(AgentState.AWAITING_CONFIRMATION)

        dialogueManager.handleIntent(UserIntent.ConfirmYes)

        assertThat(actionExecuted).isTrue()
        assertThat(dialogueManager.currentState).isEqualTo(AgentState.IDLE)
    }

    @Test
    fun `quando em AWAITING_CONFIRMATION e receber ConfirmNo, cancela sem executar`() {
        var actionExecuted = false
        dialogueManager.setPendingConfirmation(
            prompt = "Você disse: 'Oi' para Gabriel. Posso enviar?",
            onConfirm = { actionExecuted = true }
        )

        dialogueManager.handleIntent(UserIntent.ConfirmNo)

        assertThat(actionExecuted).isFalse()
        assertThat(dialogueManager.currentState).isEqualTo(AgentState.IDLE)
    }
}
