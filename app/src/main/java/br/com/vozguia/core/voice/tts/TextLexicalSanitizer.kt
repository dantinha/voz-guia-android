package br.com.vozguia.core.voice.tts

/**
 * Sanitizador léxico para fala humanizada.
 * Remove termos técnicos do TalkBack, URLs cruas, códigos hexadecimais
 * e emojis repetidos antes de enviar ao sintetizador de voz.
 */
class TextLexicalSanitizer {

    private val urlRegex = Regex("""(?i)\b(?:https?://|www\.)\S+""")
    private val hexCodeRegex = Regex("""(?:(?<!\w)0x[0-9a-fA-F]+\b|#[0-9a-fA-F]{3,8}\b)""")

    private val talkBackJargonPatterns = listOf(
        Regex("""(?i)\btoque duas vezes para (?:ativar|abrir|selecionar)\b[,.]?"""),
        Regex("""(?i)\bbotão (?:não )?marcado\b[,.]?"""),
        Regex("""(?i)\bcaixa de seleção (?:não )?marcada\b[,.]?"""),
        Regex("""(?i)\blista com \d+ itens\b[,.]?"""),
        Regex("""(?i)\bícone de\b"""),
        Regex("""(?i)\bícone\b"""),
        Regex("""(?i)\bdeslize com dois dedos\b[,.]?""")
    )

    // Regex para caracteres emojis repetidos consecutivamente
    private val repeatedEmojiRegex = Regex("""([\uD83C-\uDBFF\uDC00-\uDFFF]){2,}""")

    fun sanitize(text: String): String {
        var result = text

        // 1. Substitui URLs por texto amigável evitando duplicação "link link"
        result = result.replace(urlRegex, "link da internet")
            .replace(Regex("""(?i)\blink\s+link\b"""), "link")

        // 2. Remove códigos hexadecimais
        result = result.replace(hexCodeRegex, "")

        // 3. Remove jargões do TalkBack
        for (pattern in talkBackJargonPatterns) {
            result = result.replace(pattern, "")
        }

        // 4. Colapsa ou remove emojis repetidos
        result = result.replace(repeatedEmojiRegex, "")

        // 5. Limpa espaços duplos e pontuação duplicada
        result = result.replace(Regex("""\s+"""), " ")
            .replace(Regex("""\s+([.,?!])"""), "$1")
            .replace(Regex("""[.,?!]{2,}"""), ".")
            .trim()

        return result
    }
}
