package br.com.vozguia.service.notification

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import br.com.vozguia.core.actions.whatsapp.WhatsAppNotificationHolder
import br.com.vozguia.core.actions.whatsapp.WhatsAppNotificationItem

class VozGuiaNotificationListenerService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return

        // Guardrail de Privacidade: Descarte imediato em memória de pacotes que não sejam o WhatsApp
        if (sbn.packageName != "com.whatsapp") {
            return
        }

        val extras = sbn.notification.extras ?: return
        val title = extras.getCharSequence("android.title")?.toString() ?: "WhatsApp"
        val text = extras.getCharSequence("android.text")?.toString() ?: ""
        val isGroup = extras.containsKey("android.isGroupConversation") && extras.getBoolean("android.isGroupConversation")

        val isVoiceNote = text.contains("Mensagem de voz", ignoreCase = true) ||
                text.contains("Áudio", ignoreCase = true) ||
                text.matches(Regex(""".*\d+:\d+.*"""))

        val item = WhatsAppNotificationItem(
            key = sbn.key,
            sender = title,
            messageText = text,
            isVoiceNote = isVoiceNote,
            isGroup = isGroup,
            timestampMs = sbn.postTime
        )

        WhatsAppNotificationHolder.repository.addNotification(item)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        if (sbn != null) {
            WhatsAppNotificationHolder.repository.removeNotification(sbn.key)
        }
    }
}
