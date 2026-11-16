package br.com.vozguia.core.actions.phone

/**
 * Contato cadastrado no Grafo Familiar do Voz Guia.
 */
data class FamilyContact(
    val id: String,
    val name: String,
    val phoneNumber: String,
    val relationship: String,
    val nicknames: List<String> = emptyList(),
    val isTrustedFamily: Boolean = true
)

/**
 * Resultado do processo de resolução de chamada telefônica.
 */
sealed interface DialResult {
    data class Initiated(
        val contact: FamilyContact,
        val useSpeakerphone: Boolean,
        val spokenAnnouncement: String
    ) : DialResult

    data class Ambiguous(
        val candidates: List<FamilyContact>,
        val spokenPrompt: String
    ) : DialResult

    data class NotFound(
        val spokenMessage: String
    ) : DialResult
}

/**
 * Ações físicas de chamada recebida (atender ou rejeitar por botão físico).
 */
enum class IncomingCallAction {
    AcceptCall,
    RejectCall,
    Ignore
}
