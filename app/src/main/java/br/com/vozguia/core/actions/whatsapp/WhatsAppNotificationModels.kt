package br.com.vozguia.core.actions.whatsapp

/**
 * Representação imutável de uma notificação capturada do WhatsApp.
 */
data class WhatsAppNotificationItem(
    val key: String,
    val sender: String,
    val messageText: String,
    val isVoiceNote: Boolean = false,
    val isGroup: Boolean = false,
    val timestampMs: Long = System.currentTimeMillis(),
    val hasReplyAction: Boolean = false
)

/**
 * Resumo consolidado para fala humanizada do WhatsApp Copiloto.
 */
data class WhatsAppUnreadSummary(
    val primarySender: String?,
    val hasVoiceNote: Boolean,
    val spokenSummary: String,
    val totalSenders: Int
)

/**
 * Repositório em memória para armazenamento volátil de notificações do WhatsApp.
 */
interface WhatsAppNotificationRepository {
    fun addNotification(item: WhatsAppNotificationItem)
    fun removeNotification(key: String)
    fun getUnreadNotifications(includeGroups: Boolean = false): List<WhatsAppNotificationItem>
    fun getNotificationsFromSender(sender: String): List<WhatsAppNotificationItem>
    fun clear()
}

class InMemoryWhatsAppNotificationRepository : WhatsAppNotificationRepository {
    private val notifications = mutableMapOf<String, WhatsAppNotificationItem>()

    override fun addNotification(item: WhatsAppNotificationItem) {
        notifications[item.key] = item
    }

    override fun removeNotification(key: String) {
        notifications.remove(key)
    }

    override fun getUnreadNotifications(includeGroups: Boolean): List<WhatsAppNotificationItem> {
        val list = notifications.values.toList()
        return if (includeGroups) {
            list
        } else {
            list.filterNot { it.isGroup }
        }
    }

    override fun getNotificationsFromSender(sender: String): List<WhatsAppNotificationItem> {
        return notifications.values.filter {
            it.sender.equals(sender, ignoreCase = true) || it.sender.contains(sender, ignoreCase = true)
        }
    }

    override fun clear() {
        notifications.clear()
    }
}

/**
 * Ponto de acesso singleton em memória para compartilhamento entre o NotificationListenerService e AccessibilityService.
 */
object WhatsAppNotificationHolder {
    val repository: WhatsAppNotificationRepository = InMemoryWhatsAppNotificationRepository()
}
