package br.com.vozguia.core.voice.wakeword

import br.com.vozguia.core.routine.phonetic.PtBrPhonetics

/**
 * Configuração imutável de ativação e personalização do assistente de voz.
 */
data class VoiceAssistantConfig(
    val wakeWordEnabled: Boolean = true,
    val wakePhrase: String = "Voz Guia",
    val customAliases: List<String> = listOf("Oi Guia", "Ei Guia", "Voz Guia"),
    val acknowledgementText: String = "Pode falar.",
    val vadSilenceMs: Long = 550L,
    val volumeButtonTriggerEnabled: Boolean = false
)

/**
 * Estado em tempo real do serviço de escuta para diagnóstico e feedback visual ao cuidador.
 */
data class VoiceRuntimeTelemetry(
    val isWakeServiceRunning: Boolean = false,
    val listeningPhase: String = "Desativado",
    val lastPartialText: String = "",
    val lastFinalText: String = "",
    val lastActionSummary: String = "",
    val lastLatencyMs: Long = 0L,
    val lastError: String? = null
)

data class WakeMatchResult(
    val detected: Boolean,
    val matchedPhrase: String? = null,
    val remainingCommand: String = ""
)

/**
 * Detector local de palavra de ativação (wake-word) com tolerância a variações de STT em pt-BR
 * e extração do comando em fala contínua ("Oi Voz Guia, ligar para o Gabriel").
 */
class WakeWordMatcher {

    private val greetingPrefixes = listOf("oi", "ei", "ola", "alo", "ok", "hey", "e ai")

    fun match(transcript: String, config: VoiceAssistantConfig): WakeMatchResult {
        val trimmed = transcript.trim()
        if (trimmed.isBlank()) return WakeMatchResult(detected = false)

        val candidates = buildCandidatePhrases(config)
        if (candidates.isEmpty()) return WakeMatchResult(detected = false)

        val rawWords = trimmed.split(Regex("""\s+""")).filter { it.isNotBlank() }
        val normalizedWords = rawWords.map { PtBrPhonetics.sanitizeForComparison(it) }

        // 1. Tenta match exato ou com saudação nas janelas de tokens a partir do início (e inline)
        val exactOrGreetingMatch = findWindowMatch(
            rawWords = rawWords,
            normalizedWords = normalizedWords,
            candidates = candidates,
            allowFuzzy = false
        )
        if (exactOrGreetingMatch != null) return exactOrGreetingMatch

        // 2. Fallback fonético / Levenshtein para variações de reconhecimento offline (ex: "vós guia")
        val fuzzyMatch = findWindowMatch(
            rawWords = rawWords,
            normalizedWords = normalizedWords,
            candidates = candidates,
            allowFuzzy = true
        )
        if (fuzzyMatch != null) return fuzzyMatch

        return WakeMatchResult(detected = false)
    }

    fun matchCandidates(hypotheses: List<String>, config: VoiceAssistantConfig): WakeMatchResult {
        for (hypothesis in hypotheses) {
            val res = match(hypothesis, config)
            if (res.detected) return res
        }
        return WakeMatchResult(detected = false)
    }

    private fun buildCandidatePhrases(config: VoiceAssistantConfig): List<String> {
        val rawTargets = (listOf(config.wakePhrase) + config.customAliases)
            .map { PtBrPhonetics.sanitizeForComparison(it) }
            .filter { it.length >= 3 }
            .distinct()

        val expanded = mutableListOf<String>()
        for (target in rawTargets) {
            expanded.add(target)
            for (greeting in greetingPrefixes) {
                if (!target.startsWith("$greeting ")) {
                    expanded.add("$greeting $target")
                }
            }
        }
        return expanded.distinct().sortedByDescending { it.split(" ").size }
    }

    private fun findWindowMatch(
        rawWords: List<String>,
        normalizedWords: List<String>,
        candidates: List<String>,
        allowFuzzy: Boolean
    ): WakeMatchResult? {
        for (candidate in candidates) {
            val candTokens = candidate.split(" ").filter { it.isNotBlank() }
            val windowSize = candTokens.size
            if (windowSize == 0 || windowSize > normalizedWords.size) continue

            for (startIdx in 0..(normalizedWords.size - windowSize)) {
                val sliceNormalized = normalizedWords.subList(startIdx, startIdx + windowSize).joinToString(" ")
                val matched = if (!allowFuzzy) {
                    sliceNormalized == candidate
                } else {
                    isPhoneticOrEditMatch(sliceNormalized, candidate)
                }

                if (matched) {
                    val remainder = rawWords
                        .drop(startIdx + windowSize)
                        .joinToString(" ")
                        .trimStart(',', '.', ';', ':', '-', '!', '?')
                        .trim()
                    return WakeMatchResult(
                        detected = true,
                        matchedPhrase = rawWords.subList(startIdx, startIdx + windowSize).joinToString(" "),
                        remainingCommand = remainder
                    )
                }
            }
        }
        return null
    }

    private fun isPhoneticOrEditMatch(spoken: String, candidate: String): Boolean {
        if (spoken == candidate) return true
        val minLen = minOf(spoken.length, candidate.length)
        if (minLen < 4) return false

        val spokenNoSpaces = spoken.replace(" ", "")
        val candNoSpaces = candidate.replace(" ", "")
        if (PtBrPhonetics.ptBrMetaphone(spokenNoSpaces) == PtBrPhonetics.ptBrMetaphone(candNoSpaces)) {
            return true
        }

        val maxDist = if (candNoSpaces.length >= 7) 2 else 1
        return PtBrPhonetics.levenshteinDistance(spokenNoSpaces, candNoSpaces) <= maxDist
    }
}
