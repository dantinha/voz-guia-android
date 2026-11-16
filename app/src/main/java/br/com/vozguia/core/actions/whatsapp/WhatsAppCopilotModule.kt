package br.com.vozguia.core.actions.whatsapp

import br.com.vozguia.core.domain.ScreenSnapshot
import br.com.vozguia.core.domain.SemanticElement

/**
 * Módulo Copiloto do WhatsApp (FEAT-3.1).
 * Gerencia a leitura de mensagens não lidas priorizando contatos VIP da família,
 * localização de áudios recebidos e envio guiado de mensagens.
 */
class WhatsAppCopilotModule(
    private val notificationRepository: WhatsAppNotificationRepository
) {

    fun summarizeUnread(vipContacts: Set<String> = emptySet()): WhatsAppUnreadSummary {
        val unread = notificationRepository.getUnreadNotifications(includeGroups = false)

        if (unread.isEmpty()) {
            return WhatsAppUnreadSummary(
                primarySender = null,
                hasVoiceNote = false,
                spokenSummary = "Você não tem nenhuma mensagem nova no WhatsApp.",
                totalSenders = 0
            )
        }

        // Agrupa por remetente
        val bySender = unread.groupBy { it.sender }

        // Ordena priorizando contatos VIP da família
        val sortedSenders = bySender.keys.sortedWith { s1, s2 ->
            val isVip1 = vipContacts.any { s1.contains(it, ignoreCase = true) }
            val isVip2 = vipContacts.any { s2.contains(it, ignoreCase = true) }
            when {
                isVip1 && !isVip2 -> -1
                !isVip1 && isVip2 -> 1
                else -> 0
            }
        }

        val primarySender = sortedSenders.firstOrNull()
        val primaryMessages = primarySender?.let { bySender[it] } ?: emptyList()
        val hasVoiceNote = primaryMessages.any { it.isVoiceNote }

        val summaryBuilder = StringBuilder()

        val firstSender = sortedSenders[0]
        val firstItems = bySender[firstSender] ?: emptyList()
        val firstVoiceCount = firstItems.count { it.isVoiceNote }

        if (firstVoiceCount > 0) {
            summaryBuilder.append("$firstSender mandou $firstVoiceCount mensagem de voz")
        } else {
            val textSample = firstItems.lastOrNull()?.messageText ?: ""
            summaryBuilder.append("$firstSender mandou: '$textSample'")
        }

        if (sortedSenders.size > 1) {
            val secondSender = sortedSenders[1]
            val secondItems = bySender[secondSender] ?: emptyList()
            val secondVoiceCount = secondItems.count { it.isVoiceNote }

            summaryBuilder.append(", e ")
            if (secondVoiceCount > 0) {
                summaryBuilder.append("$secondSender mandou $secondVoiceCount mensagem de voz")
            } else {
                val textSample = secondItems.lastOrNull()?.messageText ?: ""
                summaryBuilder.append("$secondSender mandou: '$textSample'")
            }
        }

        val remaining = sortedSenders.size - 2
        if (remaining > 0) {
            summaryBuilder.append(", além de outras $remaining conversas")
        }

        if (hasVoiceNote) {
            summaryBuilder.append(". Quer ouvir o áudio do $firstSender agora?")
        } else {
            summaryBuilder.append(". Quer que eu responda para o $firstSender?")
        }

        return WhatsAppUnreadSummary(
            primarySender = primarySender,
            hasVoiceNote = hasVoiceNote,
            spokenSummary = summaryBuilder.toString(),
            totalSenders = sortedSenders.size
        )
    }

    fun findVoiceNoteToPlay(screen: ScreenSnapshot): SemanticElement? {
        if (screen.packageName != "com.whatsapp") return null

        return screen.actionableElements.firstOrNull { element ->
            val desc = element.contentDescription ?: ""
            val text = element.text ?: ""
            element.isClickable && (
                    desc.contains("Reproduzir mensagem de voz", ignoreCase = true) ||
                    desc.contains("Reproduzir", ignoreCase = true) ||
                    text.contains("Mensagem de voz", ignoreCase = true)
            )
        }
    }

    fun findSendButton(screen: ScreenSnapshot): SemanticElement? {
        if (screen.packageName != "com.whatsapp") return null

        return screen.actionableElements.firstOrNull { element ->
            val id = element.id ?: ""
            val desc = element.contentDescription ?: ""
            val text = element.text ?: ""
            element.isClickable && (
                    id.endsWith(":id/send") ||
                    desc.equals("Enviar", ignoreCase = true) ||
                    desc.equals("Send", ignoreCase = true) ||
                    text.equals("Enviar", ignoreCase = true)
            )
        }
    }

    fun findMessageInputField(screen: ScreenSnapshot): SemanticElement? {
        if (screen.packageName != "com.whatsapp") return null

        return screen.actionableElements.firstOrNull { element ->
            val id = element.id ?: ""
            val text = element.text ?: ""
            val desc = element.contentDescription ?: ""
            id.endsWith(":id/entry") ||
                    text.contains("Mensagem", ignoreCase = true) ||
                    desc.contains("Mensagem", ignoreCase = true)
        }
    }
}
