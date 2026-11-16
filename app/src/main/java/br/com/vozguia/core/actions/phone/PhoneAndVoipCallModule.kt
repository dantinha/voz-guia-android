package br.com.vozguia.core.actions.phone

import java.text.Normalizer

/**
 * Módulo de Telefone e Chamadas Unificadas (FEAT-3.2).
 * Discagem por apelidos familiares, viva-voz automático e atendimento/rejeição por botão físico.
 */
class PhoneAndVoipCallModule {

    fun resolveCallTarget(
        query: String,
        contacts: List<FamilyContact>,
        isHeadsetConnected: Boolean = false
    ): DialResult {
        val cleanQuery = normalize(query)
        if (cleanQuery.isBlank()) {
            return DialResult.NotFound("Para quem você gostaria de ligar?")
        }

        // Busca exata ou por apelido/relação
        val matches = contacts.filter { contact ->
            val matchName = normalize(contact.name).contains(cleanQuery)
            val matchRel = normalize(contact.relationship).contains(cleanQuery)
            val matchNick = contact.nicknames.any { normalize(it).contains(cleanQuery) }
            matchName || matchRel || matchNick
        }

        return when {
            matches.isEmpty() -> {
                DialResult.NotFound("Não encontrei o contato $query na sua agenda de família.")
            }

            matches.size == 1 -> {
                val contact = matches[0]
                val useSpeakerphone = !isHeadsetConnected
                val announcement = if (useSpeakerphone) {
                    "Ligando para ${contact.name} no viva-voz."
                } else {
                    "Ligando para ${contact.name}."
                }
                DialResult.Initiated(contact, useSpeakerphone, announcement)
            }

            else -> {
                val candidateNames = matches.map { it.name }.joinToString(" e ")
                val prompt = "Encontrei $candidateNames. Para qual dos dois você quer ligar?"
                DialResult.Ambiguous(matches, prompt)
            }
        }
    }

    fun formatIncomingCallAnnouncement(callerName: String): String {
        return "Ligação de $callerName. Aperte o botão de volume para atender, ou segure para recusar."
    }

    fun handleVolumeKeyDuringRinging(isShortTap: Boolean): IncomingCallAction {
        return if (isShortTap) {
            IncomingCallAction.AcceptCall
        } else {
            IncomingCallAction.RejectCall
        }
    }

    private fun normalize(text: String): String {
        val noAccents = Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace(Regex("""\p{M}"""), "")
        return noAccents.lowercase()
            .replace(Regex("""[^\w\s]"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }
}
