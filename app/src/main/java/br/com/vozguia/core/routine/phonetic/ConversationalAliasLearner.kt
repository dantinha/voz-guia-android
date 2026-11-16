package br.com.vozguia.core.routine.phonetic

import br.com.vozguia.core.routine.data.TrustedContactRepository
import br.com.vozguia.core.routine.domain.TrustedContact

sealed interface AliasLearningState {
    data class AwaitingCandidateName(
        val role: String,
        val spokenPrompt: String
    ) : AliasLearningState

    data class AwaitingConfirmation(
        val role: String,
        val candidateContact: TrustedContact,
        val spokenPrompt: String
    ) : AliasLearningState

    data class LearnedSuccessfully(
        val updatedContact: TrustedContact,
        val spokenConfirmation: String
    ) : AliasLearningState

    data class ContactNotFoundInAgenda(
        val role: String,
        val spokenPrompt: String
    ) : AliasLearningState

    data object Cancelled : AliasLearningState
}

class ConversationalAliasLearner(
    private val contactRepository: TrustedContactRepository
) {

    private var currentRole: String? = null
    private var pendingCandidate: TrustedContact? = null

    /**
     * Inicia a sessão de aprendizado quando o usuário usa um parentesco desconhecido.
     */
    fun startLearning(unmappedRole: String): AliasLearningState {
        currentRole = unmappedRole.trim().lowercase()
        pendingCandidate = null
        val prompt = "Qual é o nome do seu $currentRole na agenda?"
        return AliasLearningState.AwaitingCandidateName(currentRole!!, prompt)
    }

    /**
     * Processa a resposta do usuário dizendo o nome do contato.
     */
    suspend fun processCandidateName(candidateName: String): AliasLearningState {
        val role = currentRole ?: return AliasLearningState.Cancelled
        val cleanCandidate = candidateName.trim().lowercase()

        val allContacts = contactRepository.getAllContacts()
        val match = allContacts.find {
            it.displayName.lowercase().contains(cleanCandidate) ||
                    it.phoneticAliases.any { alias -> alias.lowercase() == cleanCandidate }
        }

        return if (match != null) {
            pendingCandidate = match
            val prompt = "Encontrei ${match.displayName}. Posso salvar que ele é seu $role?"
            AliasLearningState.AwaitingConfirmation(role, match, prompt)
        } else {
            val prompt = "Não encontrei ninguém chamado $candidateName na agenda. Gostaria de tentar outro nome?"
            AliasLearningState.ContactNotFoundInAgenda(role, prompt)
        }
    }

    /**
     * Confirma ou rejeita a associação do apelido ao contato.
     */
    suspend fun confirmAssociation(confirmed: Boolean): AliasLearningState {
        val role = currentRole ?: return AliasLearningState.Cancelled
        val candidate = pendingCandidate ?: return AliasLearningState.Cancelled

        return if (confirmed) {
            val newAliases = candidate.phoneticAliases.toMutableList()
            if (!newAliases.contains(role)) newAliases.add(role)
            if (!newAliases.contains("meu $role") && !newAliases.contains("minha $role")) {
                newAliases.add("meu $role")
            }

            val updated = candidate.copy(
                relationshipRole = if (candidate.relationshipRole == "amigo" || candidate.relationshipRole.isBlank()) role else candidate.relationshipRole,
                phoneticAliases = newAliases.toList(),
                isVipFamily = true
            )
            contactRepository.updateContact(updated)

            currentRole = null
            pendingCandidate = null

            val confirmationText = "Entendido! Salvei que ${updated.displayName} é seu $role."
            AliasLearningState.LearnedSuccessfully(updated, confirmationText)
        } else {
            currentRole = null
            pendingCandidate = null
            AliasLearningState.Cancelled
        }
    }
}
