package br.com.vozguia.core.routine.phonetic

import java.text.Normalizer
import kotlin.math.min

/**
 * Utilitários compartilhados de normalização, fonética pt-BR e distância de edição (Levenshtein).
 * Utilizados na resolução de contatos familiares e no reconhecimento de palavra de ativação (wake-word).
 */
object PtBrPhonetics {

    fun sanitizeForComparison(text: String): String {
        val normalized = Normalizer.normalize(text.trim().lowercase(), Normalizer.Form.NFD)
        return normalized.replace("\\p{InCombiningDiacriticalMarks}".toRegex(), "")
            .replace("[^a-z0-9 ]".toRegex(), " ")
            .replace("\\s+".toRegex(), " ")
            .trim()
    }

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

        // 'l' no final de palavra ou antes de consoante tem som de 'u' no pt-BR
        s = s.replace("l\\b".toRegex(), "u")
        s = s.replace("l(?=[bcdfghjkpqrstvwxz])".toRegex(), "u")

        // 's' entre vogais tem som de 'z'
        s = s.replace("(?<=[aeiou])s(?=[aeiou])".toRegex(), "z")

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
