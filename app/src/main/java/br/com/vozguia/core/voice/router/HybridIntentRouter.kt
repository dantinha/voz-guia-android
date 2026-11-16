package br.com.vozguia.core.voice.router

import br.com.vozguia.core.domain.UserIntent
import java.text.Normalizer

/**
 * Roteador de Intenção Híbrido em 3 Camadas do Voz Guia:
 * 1. Camada 1: Fast-Path Determinístico Local (< 50ms, 0 tokens, R$ 0,00)
 * 2. Camada 2: Extrator Local de Entidades e Parâmetros
 * 3. Camada 3: Encaminhamento para Inteligência Multimodal em Nuvem
 */
class HybridIntentRouter {

    fun route(rawTranscription: String): UserIntent {
        val trimmed = rawTranscription.trim()
        if (trimmed.isBlank()) {
            return UserIntent.UnknownIntent(rawTranscription)
        }

        val normalized = normalize(trimmed)

        // ==========================================
        // 1. FAST-PATH: Emergência, Parada e Repetição
        // ==========================================

        if (matchesPattern(normalized, EMERGENCY_SOS_PATTERNS)) {
            return UserIntent.EmergencySos
        }

        if (matchesPattern(normalized, STOP_PATTERNS)) {
            return UserIntent.StopOrCancel
        }

        if (matchesPattern(normalized, SKIP_AD_PATTERNS)) {
            return UserIntent.YouTubeSkipAd
        }

        // Verificado antes de ConfirmNo para que "não entendi" não caia em "não"
        if (matchesPattern(normalized, REPEAT_PATTERNS)) {
            return UserIntent.RepeatLast
        }

        // ==========================================
        // 2. WHATSAPP, TELEFONIA & YOUTUBE (Extração Estruturada)
        // Verificados antes de confirmações curtas ("manda", "sim", "não")
        // ==========================================

        if (matchesPattern(normalized, WA_UNREAD_PATTERNS)) {
            return UserIntent.ReadUnreadWhatsAppMessages
        }

        val playAudioMatch = PLAY_AUDIO_WITH_CONTACT_REGEX.find(trimmed)
        if (playAudioMatch != null) {
            val contact = playAudioMatch.groups["contact"]?.value?.trim()
            return UserIntent.PlayWhatsAppAudio(contactName = contact)
        }

        if (matchesPattern(normalized, WA_PLAY_AUDIO_GENERIC_PATTERNS)) {
            return UserIntent.PlayWhatsAppAudio(contactName = null)
        }

        val sendWaMatch = SEND_WA_REGEX.find(trimmed)
        if (sendWaMatch != null) {
            val contact = sendWaMatch.groups["contact"]?.value?.trim() ?: ""
            val message = sendWaMatch.groups["msg"]?.value?.trim() ?: ""
            if (contact.isNotBlank() && message.isNotBlank()) {
                return UserIntent.SendWhatsAppMessage(targetContact = contact, messageText = message)
            }
        }

        val callMatch = CALL_REGEX.find(trimmed)
        if (callMatch != null) {
            val contact = callMatch.groups["contact"]?.value?.trim() ?: ""
            if (contact.isNotBlank()) {
                return UserIntent.DirectCall(targetName = contact)
            }
        }

        val ytMatch = YT_SEARCH_REGEX.find(trimmed)
        if (ytMatch != null) {
            val query = ytMatch.groups["query"]?.value?.trim() ?: ""
            if (query.isNotBlank()) {
                return UserIntent.YouTubeSearchAndPlay(query = query)
            }
        }

        // ==========================================
        // 3. CONFIRMAÇÕES DIÁLOGO 2FA E CONTROLE DE MÍDIA
        // ==========================================

        // ConfirmNo ANTES de ConfirmYes para que "não manda" não dispare "manda"
        if (matchesPattern(normalized, CONFIRM_NO_PATTERNS)) {
            return UserIntent.ConfirmNo
        }

        if (matchesPattern(normalized, CONFIRM_YES_PATTERNS)) {
            return UserIntent.ConfirmYes
        }

        if (matchesPattern(normalized, PAUSE_PATTERNS)) {
            return UserIntent.PauseMedia
        }

        if (matchesPattern(normalized, NEXT_PATTERNS)) {
            return UserIntent.NextMedia
        }

        if (matchesPattern(normalized, RESUME_PATTERNS)) {
            return UserIntent.ResumeMedia
        }

        // ==========================================
        // 4. UTILIDADES LOCAIS & ASSISTÊNCIA VISUAL
        // ==========================================

        if (matchesPattern(normalized, TIME_PATTERNS)) {
            return UserIntent.TimeQuery
        }

        if (matchesPattern(normalized, DATE_PATTERNS)) {
            return UserIntent.DateQuery
        }

        if (matchesPattern(normalized, BATTERY_PATTERNS)) {
            return UserIntent.BatteryQuery
        }

        if (matchesPattern(normalized, DESCRIBE_IMAGE_PATTERNS)) {
            return UserIntent.DescribeReceivedImage()
        }

        if (isGeneralQuestion(normalized)) {
            return UserIntent.GeneralQuestion(query = trimmed)
        }

        return UserIntent.UnknownIntent(rawTranscription = trimmed)
    }

    private fun normalize(text: String): String {
        val noAccents = Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace(Regex("""\p{M}"""), "")
        return noAccents.lowercase()
            .replace(Regex("""[^\w\s]"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    private fun matchesPattern(text: String, patterns: List<Regex>): Boolean {
        return patterns.any { it.containsMatchIn(text) }
    }

    private fun isGeneralQuestion(normalized: String): Boolean {
        val questionKeywords = listOf("quem", "o que e", "onde", "quando", "como funciona", "por que")
        return questionKeywords.any { normalized.startsWith(it) || normalized.contains(" $it ") }
    }

    companion object {
        private val EMERGENCY_SOS_PATTERNS = listOf(
            Regex("""\b(?:socorro|emergencia|preciso de ajuda|me ajuda|chamar socorro|chama socorro|estou passando mal|ajuda urgente)\b""")
        )

        private val STOP_PATTERNS = listOf(
            Regex("""\b(?:para|parar|cancela|cancelar|cala a boca|para de falar|silencio)\b""")
        )

        private val SKIP_AD_PATTERNS = listOf(
            Regex("""\b(?:pula|pular)\s+(?:o\s+)?anuncio\b""")
        )

        private val REPEAT_PATTERNS = listOf(
            Regex("""\b(?:repete|repita|nao entendi|o que voce disse|como e)\b""")
        )

        private val CONFIRM_NO_PATTERNS = listOf(
            Regex("""\b(?:nao manda|nao enviar|nao|deixa quieto|espera)\b""")
        )

        private val CONFIRM_YES_PATTERNS = listOf(
            Regex("""^(?:sim|pode mandar|manda|pode enviar|isso|confirmo|enviar|mande)$"""),
            Regex("""\b(?:sim|pode mandar|pode enviar|confirmo)\b""")
        )

        private val PAUSE_PATTERNS = listOf(
            Regex("""\b(?:pausa|pausar)\b""")
        )

        private val RESUME_PATTERNS = listOf(
            Regex("""\b(?:continua|continua tocando|retomar|despausa)\b"""),
            Regex("""^toca$""")
        )

        private val NEXT_PATTERNS = listOf(
            Regex("""\b(?:proximo|proxima|pula|avanca)\b""")
        )

        private val TIME_PATTERNS = listOf(
            Regex("""\b(?:que horas sao|que hora e|hora certa)\b"""),
            Regex("""^horas$""")
        )

        private val DATE_PATTERNS = listOf(
            Regex("""\b(?:que dia e hoje|qual e a data|data de hoje|dia do mes)\b""")
        )

        private val BATTERY_PATTERNS = listOf(
            Regex("""\b(?:quanta bateria|nivel de bateria|porcentagem de bateria|bateria do celular)\b""")
        )

        private val WA_UNREAD_PATTERNS = listOf(
            Regex("""\b(?:tem mensagem nova|mensagens novas|o que mandaram|ler mensagens)\b""")
        )

        private val WA_PLAY_AUDIO_GENERIC_PATTERNS = listOf(
            Regex("""\b(?:toca o audio|toca audio|ouvir audio)\b""")
        )

        private val DESCRIBE_IMAGE_PATTERNS = listOf(
            Regex("""\b(?:o que tem nessa foto|descreve(?:r)? a foto|descreve(?:r)? a imagem|foto que me mandaram|o que e isso na foto)\b""")
        )

        private val PLAY_AUDIO_WITH_CONTACT_REGEX = Regex(
            """(?i)\b(?:toca|ouvir?)\s+(?:o\s+)?áudio\s+(?:do|da|de)\s+(?<contact>[\p{L}\s]+)"""
        )

        private val SEND_WA_REGEX = Regex(
            """(?i)\b(?:manda|enviar?)\s+(?:uma?\s+)?(?:mensagem|zap|msg)\s+(?:pro|pra|para\s+o|para\s+a|para)\s+(?<contact>[\p{L}\s]+?)\s+(?:(?:dizendo|falando|avisando)(?:\s+que)?|que)\s+(?<msg>.+)"""
        )

        private val CALL_REGEX = Regex(
            """(?i)\b(?:liga|ligar?|chama|chamar?)\s+(?:pro|pra|para\s+o|para\s+a|para)\s+(?<contact>[\p{L}\s]+)"""
        )

        private val YT_SEARCH_REGEX = Regex(
            """(?i)\b(?:toca|tocar?|coloca|colocar?|põe|poe|busca|ouvir?)\s+(?<query>.+?)(?:\s+no\s+youtube|\s+no\s+yt)?$"""
        )
    }
}
