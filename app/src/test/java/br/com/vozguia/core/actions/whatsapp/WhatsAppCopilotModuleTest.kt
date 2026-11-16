package br.com.vozguia.core.actions.whatsapp

import br.com.vozguia.core.domain.ScreenRect
import br.com.vozguia.core.domain.ScreenSnapshot
import br.com.vozguia.core.domain.SemanticElement
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

class WhatsAppCopilotModuleTest {

    private lateinit var repository: InMemoryWhatsAppNotificationRepository
    private lateinit var copilot: WhatsAppCopilotModule

    @Before
    fun setUp() {
        repository = InMemoryWhatsAppNotificationRepository()
        copilot = WhatsAppCopilotModule(repository)
    }

    @Test
    fun `summarizeUnread deve priorizar contatos VIP familiares e ignorar grupos por padrao`() {
        repository.addNotification(
            WhatsAppNotificationItem(
                key = "1",
                sender = "Família Silva (Grupo)",
                messageText = "Foto de bom dia",
                isGroup = true
            )
        )
        repository.addNotification(
            WhatsAppNotificationItem(
                key = "2",
                sender = "Maria",
                messageText = "Bom dia, já tomou café?",
                isGroup = false
            )
        )
        repository.addNotification(
            WhatsAppNotificationItem(
                key = "3",
                sender = "Gabriel",
                messageText = "Mensagem de voz (0:45)",
                isVoiceNote = true,
                isGroup = false
            )
        )

        val summary = copilot.summarizeUnread(vipContacts = setOf("Gabriel"))

        // Gabriel é VIP, deve vir em primeiro lugar
        assertThat(summary.primarySender).isEqualTo("Gabriel")
        assertThat(summary.hasVoiceNote).isTrue()
        assertThat(summary.spokenSummary).contains("Gabriel mandou 1 mensagem de voz")
        assertThat(summary.spokenSummary).contains("Maria mandou: 'Bom dia, já tomou café?'")
        // Não deve citar o grupo de bom dia no resumo principal
        assertThat(summary.spokenSummary).doesNotContain("Família Silva (Grupo)")
    }

    @Test
    fun `findVoiceNoteToPlay deve localizar no de audio nao reproduzido na tela de conversa`() {
        val screen = ScreenSnapshot(
            packageName = "com.whatsapp",
            actionableElements = listOf(
                SemanticElement(text = "Ontem 18:30"),
                SemanticElement(
                    contentDescription = "Reproduzir mensagem de voz, 45 segundos",
                    isClickable = true,
                    bounds = ScreenRect(100, 500, 200, 600)
                ),
                SemanticElement(
                    text = "Mensagem de voz",
                    isClickable = true,
                    bounds = ScreenRect(100, 700, 200, 800)
                )
            )
        )

        val target = copilot.findVoiceNoteToPlay(screen)

        assertThat(target).isNotNull()
        assertThat(target?.contentDescription).contains("Reproduzir mensagem de voz")
    }

    @Test
    fun `findSendButton deve encontrar botao de enviar com fallback resiliente`() {
        val screen = ScreenSnapshot(
            packageName = "com.whatsapp",
            actionableElements = listOf(
                SemanticElement(
                    id = "com.whatsapp:id/send",
                    contentDescription = "Enviar",
                    isClickable = true,
                    bounds = ScreenRect(900, 1800, 1000, 1900)
                )
            )
        )

        val sendButton = copilot.findSendButton(screen)

        assertThat(sendButton).isNotNull()
        assertThat(sendButton?.contentDescription).isEqualTo("Enviar")
    }
}
