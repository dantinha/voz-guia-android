package br.com.vozguia.core.caregiver

import br.com.vozguia.core.routine.data.TrustedContactRepository
import br.com.vozguia.core.routine.domain.TrustedContact

sealed interface EmergencySosResult {
    data class Executed(
        val targetContact: TrustedContact,
        val phoneNumber: String,
        val spokenAnnouncement: String,
        val maxSpeakerVolume: Boolean = true
    ) : EmergencySosResult
}

class EmergencySosDispatcher(
    private val contactRepository: TrustedContactRepository
) {

    private val samuFallback = TrustedContact(
        id = -1L,
        displayName = "SAMU - Emergência",
        phoneNumber = "192",
        relationshipRole = "emergência",
        isVipFamily = false,
        isEmergencyContact = true
    )

    /**
     * Executa a discagem de emergência imediata em viva-voz no volume máximo.
     */
    suspend fun dispatchEmergencySos(): EmergencySosResult {
        val emergencyContact = contactRepository.getEmergencyContact()
            ?: contactRepository.getAllContacts().firstOrNull { it.isVipFamily }
            ?: samuFallback

        val announcement = "Ligando agora para emergência: ${emergencyContact.displayName}."

        return EmergencySosResult.Executed(
            targetContact = emergencyContact,
            phoneNumber = emergencyContact.phoneNumber,
            spokenAnnouncement = announcement,
            maxSpeakerVolume = true
        )
    }

    /**
     * Gera o texto da mensagem de socorro a ser enviada no WhatsApp para os familiares.
     */
    fun generateEmergencyWhatsAppText(timeHhMm: String): String {
        return "🚨 [EMERGÊNCIA SOS] Papai acionou o pedido de socorro às $timeHhMm pelo Voz Guia. Por favor, atenda a ligação imediatamente."
    }
}
