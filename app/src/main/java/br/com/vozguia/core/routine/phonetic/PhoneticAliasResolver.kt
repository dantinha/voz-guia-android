package br.com.vozguia.core.routine.phonetic

import br.com.vozguia.core.routine.data.TrustedContactRepository
import br.com.vozguia.core.routine.domain.TrustedContact
import java.text.Normalizer
import kotlin.math.min

sealed interface PhoneticMatchResult {
    data class Found(
        val contact: TrustedContact,
        val confidence: Float,
        val matchedBy: String
    ) : PhoneticMatchResult

    data class NotFound(
        val query: String,
        val detectedRole: String? = null
    ) : PhoneticMatchResult
}

class PhoneticAliasResolver(
    private val contactRepository: TrustedContactRepository
) {

    private val roleDisplayMap = mapOf(
        "filho" to "filho",
        "filha" to "filha",
        "esposa" to "esposa",
        "marido" to "marido",
        "irmao" to "irmão",
        "irma" to "irmã",
        "mae" to "mãe",
        "pai" to "pai",
        "neto" to "neto",
        "neta" to "neta",
        "tio" to "tio",
        "tia" to "tia",
        "primo" to "primo",
        "prima" to "prima",
        "sobrinho" to "sobrinho",
        "sobrinha" to "sobrinha",
        "medico" to "médico",
        "doutor" to "doutor",
        "doutora" to "doutora"
    )

    private val phrasePrefixes = listOf(
        "manda mensagem para o", "manda mensagem para a", "manda mensagem pro", "manda mensagem pra",
        "manda zap para o", "manda zap para a", "manda zap pro", "manda zap pra",
        "mandar mensagem para o", "mandar mensagem para a", "mandar mensagem pro", "mandar mensagem pra",
        "enviar mensagem para o", "enviar mensagem para a", "enviar mensagem pro", "enviar mensagem pra",
        "liga para o", "liga para a", "liga pro", "liga pra",
        "ligar para o", "ligar para a", "ligar pro", "ligar pra",
        "chama o", "chama a", "chamar o", "chamar a",
        "falar com o", "falar com a", "falar com",
        "para o", "para a", "pro", "pra", "com o", "com a", "com"
    )

    /**
     * Resolve uma menção direta (nome, apelido ou papel de parentesco) para um contato confiável.
     */
    suspend fun resolve(query: String): PhoneticMatchResult {
        val cleanQuery = sanitizeForComparison(query)
        if (cleanQuery.isBlank()) {
            return PhoneticMatchResult.NotFound(query)
        }

        val allContacts = contactRepository.getAllContacts()
        if (allContacts.isEmpty()) {
            return PhoneticMatchResult.NotFound(query, detectRole(cleanQuery))
        }

        // 1º Passo: Match exato de papel (ex: "filho", "esposa")
        for (contact in allContacts) {
            val contactRole = sanitizeForComparison(contact.relationshipRole)
            if (contactRole == cleanQuery || "meu $contactRole" == cleanQuery || "minha $contactRole" == cleanQuery) {
                return PhoneticMatchResult.Found(contact, 1.0f, "EXACT_ROLE")
            }
        }

        // 2º Passo: Match exato de displayName ou de algum apelido
        for (contact in allContacts) {
            val nameSanitized = sanitizeForComparison(contact.displayName)
            if (nameSanitized == cleanQuery || nameSanitized.split(" ").contains(cleanQuery)) {
                return PhoneticMatchResult.Found(contact, 0.98f, "EXACT_NAME")
            }
            for (alias in contact.phoneticAliases) {
                val aliasSanitized = sanitizeForComparison(alias)
                if (aliasSanitized == cleanQuery || "meu $aliasSanitized" == cleanQuery || "minha $aliasSanitized" == cleanQuery) {
                    return PhoneticMatchResult.Found(contact, 0.95f, "EXACT_ALIAS")
                }
            }
        }

        // 3º Passo: Match Fonético Metaphone pt-BR (ex: "gabrieu" == "gabriel")
        val metaphoneQuery = ptBrMetaphone(cleanQuery)
        for (contact in allContacts) {
            if (ptBrMetaphone(sanitizeForComparison(contact.displayName)) == metaphoneQuery) {
                return PhoneticMatchResult.Found(contact, 0.90f, "METAPHONE_NAME")
            }
            for (alias in contact.phoneticAliases) {
                if (ptBrMetaphone(sanitizeForComparison(alias)) == metaphoneQuery) {
                    return PhoneticMatchResult.Found(contact, 0.88f, "METAPHONE_ALIAS")
                }
            }
        }

        // 4º Passo: Distância Levenshtein <= 2
        var bestContact: TrustedContact? = null
        var bestDistance = Int.MAX_VALUE
        var matchedSource = ""

        for (contact in allContacts) {
            val nameWords = sanitizeForComparison(contact.displayName).split(" ")
            for (word in nameWords) {
                val dist = levenshteinDistance(cleanQuery, word)
                if (dist <= 2 && dist < bestDistance) {
                    bestDistance = dist
                    bestContact = contact
                    matchedSource = "LEVENSHTEIN_NAME"
                }
            }
            for (alias in contact.phoneticAliases) {
                val aliasSanitized = sanitizeForComparison(alias)
                val dist = levenshteinDistance(cleanQuery, aliasSanitized)
                if (dist <= 2 && dist < bestDistance) {
                    bestDistance = dist
                    bestContact = contact
                    matchedSource = "LEVENSHTEIN_ALIAS"
                }
            }
        }

        if (bestContact != null) {
            val confidence = when (bestDistance) {
                0 -> 1.0f
                1 -> 0.85f
                2 -> 0.75f
                else -> 0.60f
            }
            return PhoneticMatchResult.Found(bestContact, confidence, matchedSource)
        }

        return PhoneticMatchResult.NotFound(query, detectRole(cleanQuery))
    }

    /**
     * Extrai a menção ao contato de frases naturais faladas e resolve.
     * Ex: "Liga pro meu filho" -> query: "meu filho" -> Gabriel
     */
    suspend fun resolveFromSpokenPhrase(phrase: String): PhoneticMatchResult {
        var clean = sanitizeForComparison(phrase)

        // Remove prefixos verbais comuns
        for (prefix in phrasePrefixes.sortedByDescending { it.length }) {
            val prefixClean = sanitizeForComparison(prefix)
            if (clean.startsWith(prefixClean)) {
                clean = clean.removePrefix(prefixClean).trim()
                break
            }
        }

        // Se sobrou algo como "o gabriel" ou "a maria", remove o artigo
        if (clean.startsWith("o ") || clean.startsWith("a ")) {
            clean = clean.substring(2).trim()
        }

        return resolve(clean)
    }

    private fun detectRole(cleanText: String): String? {
        val words = cleanText.split(" ")
        for ((normalizedRole, displayRole) in roleDisplayMap) {
            if (words.contains(normalizedRole) || cleanText.contains(normalizedRole)) {
                return displayRole
            }
        }
        return null
    }

    /**
     * Sanitiza removendo acentuação, pontuação e múltiplos espaços.
     */
    fun sanitizeForComparison(text: String): String {
        val normalized = Normalizer.normalize(text.trim().lowercase(), Normalizer.Form.NFD)
        return normalized.replace("\\p{InCombiningDiacriticalMarks}".toRegex(), "")
            .replace("[^a-z0-9 ]".toRegex(), " ")
            .replace("\\s+".toRegex(), " ")
            .trim()
    }

    /**
     * Algoritmo de normalização fonética adaptado para o português brasileiro:
     * - 'l' em fim de sílaba ou palavra soa como 'u' ("gabriel" -> "gabrieu")
     * - 'ph' -> 'f'
     * - 'ch'/'x' -> 'x'
     * - 'ç' / 'c' antes de 'e', 'i' -> 's'
     * - 'c' antes de 'a', 'o', 'u' ou 'qu'/'k' -> 'k'
     * - 'lh' -> 'li', 'nh' -> 'ni'
     * - deduplica caracteres consecutivos
     */
    fun ptBrMetaphone(text: String): String {
        if (text.isEmpty()) return ""
        var s = sanitizeForComparison(text)

        s = s.replace("ph", "f")
        s = s.replace("ç", "s")
        s = s.replace("ce", "se").replace("ci", "si")
        s = s.replace("ca", "ka").replace("co", "ka").replace("cu", "ka")
        s = s.replace("que", "ke").replace("qui", "ki").replace("qu", "k")
        s = s.replace("ch", "x").replace("sh", "x")
        s = s.replace("lh", "li").replace("nh", "ni")
        s = s.replace("w", "v").replace("y", "i")

        // 'l' no final de palavra ou antes de consoante -> 'u'
        s = s.replace("l\\b".toRegex(), "u")
        s = s.replace("l(?=[bcdfghjkpqrstvwxz])".toRegex(), "u")

        // 's' entre vogais tem som de 'z'
        s = s.replace("(?<=[aeiou])s(?=[aeiou])".toRegex(), "z")

        // Reduz consoantes repetidas
        val sb = StringBuilder()
        var lastChar = ' '
        for (c in s) {
            if (c != lastChar || c.isWhitespace() || "aeiou".contains(c)) {
                sb.append(c)
                lastChar = c
            }
        }

        return sb.toString()
    }

    /**
     * Cálculo de distância Levenshtein.
     */
    fun levenshteinDistance(s1: String, s2: String): Int {
        if (s1 == s2) return 0
        if (s1.isEmpty()) return s2.length
        if (s2.isEmpty()) return s1.length

        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }

        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j

        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = min(
                    min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        return dp[s1.length][s2.length]
    }
}
