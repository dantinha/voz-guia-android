package br.com.vozguia.core.guardrails.impl

import br.com.vozguia.core.domain.ScreenSnapshot
import br.com.vozguia.core.domain.UserIntent
import br.com.vozguia.core.guardrails.GuardrailVerdict
import br.com.vozguia.core.guardrails.VozGuiaGuardrail

/**
 * Garante que a automação de toques e navegação fique estritamente restrita
 * à lista fechada de pacotes homologados (WhatsApp, Telefone, YouTube, Relógio/Alarmes e o próprio app).
 * Prioridade: 90.
 */
class AccessBoundaryGuardrail(
    customAllowedPackages: Set<String> = emptySet()
) : VozGuiaGuardrail {

    override val name: String = "AccessBoundaryGuardrail"
    override val priority: Int = 90

    private val defaultAllowedPackages = setOf(
        "com.whatsapp",
        "com.google.android.dialer",
        "com.android.dialer",
        "com.android.server.telecom",
        "com.google.android.youtube",
        "com.google.android.deskclock",
        "com.android.deskclock",
        "br.com.vozguia",
        "br.com.gabriel.vozguia"
    )

    private val allowedPackages: Set<String> = defaultAllowedPackages + customAllowedPackages

    private val webUrlRegex = Regex(
        """(?i)\b(?:https?://|www\.)\S+|\b[a-zA-Z0-9.-]+\.(?:com\.br|com|org|net|gov\.br|gov|edu|app|io|me|ly|gl|tv|xyz)\b(?::\d+)?(?:/[^\s]*)?"""
    )

    override suspend fun evaluate(
        intent: UserIntent,
        currentScreen: ScreenSnapshot?
    ): GuardrailVerdict {
        // Se a tela atual é conhecida e não é permitida nem tela de sistema (launcher/dialog)
        val currentPackage = currentScreen?.packageName
        if (currentPackage != null && !currentScreen.isSystemDialog) {
            val isLauncher = currentPackage.contains("launcher", ignoreCase = true)
            if (!isLauncher && !allowedPackages.contains(currentPackage)) {
                // Comandos globais que não tocam na tela do app desconhecido são sempre permitidos
                val isGlobalHarmlessQuery = intent is UserIntent.EmergencySos ||
                        intent is UserIntent.TimeQuery ||
                        intent is UserIntent.DateQuery ||
                        intent is UserIntent.BatteryQuery ||
                        intent is UserIntent.StopOrCancel ||
                        intent is UserIntent.RepeatLast ||
                        intent is UserIntent.PauseMedia ||
                        intent is UserIntent.ResumeMedia ||
                        intent is UserIntent.NextMedia ||
                        intent is UserIntent.ReadUnreadWhatsAppMessages ||
                        intent is UserIntent.DescribeScreenContext ||
                        intent is UserIntent.SleepMode

                // Intenções que lançam WhatsApp, Telefone ou YouTube diretamente via Intent do sistema
                val isIntentBasedLaunch = intent is UserIntent.SendWhatsAppMessage ||
                        intent is UserIntent.OpenWhatsAppChat ||
                        intent is UserIntent.SendWhatsAppVoiceNote ||
                        intent is UserIntent.DirectCall ||
                        intent is UserIntent.YouTubeSearchAndPlay

                if (!isGlobalHarmlessQuery && !isIntentBasedLaunch) {
                    return GuardrailVerdict.Blocked(
                        reason = "Aplicativo em primeiro plano não autorizado para interação: $currentPackage",
                        spokenMessage = "O Voz Guia só pode interagir com WhatsApp, Telefone e YouTube."
                    )
                }
            }
        }

        // Bloqueio robusto de envio de links da internet nas mensagens ditadas
        if (intent is UserIntent.SendWhatsAppMessage) {
            val containsUrl = webUrlRegex.containsMatchIn(intent.messageText)
            if (containsUrl) {
                return GuardrailVerdict.Blocked(
                    reason = "Tentativa de envio de link web não permitido",
                    spokenMessage = "Por segurança, não é permitido ditar links da internet em mensagens."
                )
            }
        }

        return GuardrailVerdict.Allowed
    }
}
