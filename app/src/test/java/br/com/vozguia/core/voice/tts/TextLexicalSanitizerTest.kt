package br.com.vozguia.core.voice.tts

import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

class TextLexicalSanitizerTest {

    private lateinit var sanitizer: TextLexicalSanitizer

    @Before
    fun setUp() {
        sanitizer = TextLexicalSanitizer()
    }

    @Test
    fun `deve remover jargoes mecanicos do TalkBack e Android`() {
        val raw = "Botão não marcado, toque duas vezes para ativar. Mensagem de Gabriel."
        val sanitized = sanitizer.sanitize(raw)

        assertThat(sanitized).doesNotContain("Botão não marcado")
        assertThat(sanitized).doesNotContain("toque duas vezes para ativar")
        assertThat(sanitized).contains("Mensagem de Gabriel.")
    }

    @Test
    fun `deve substituir URLs cruas por texto amigavel ou omitir`() {
        val raw = "Veja o link https://noticias.globo.com/politica para mais informações."
        val sanitized = sanitizer.sanitize(raw)

        assertThat(sanitized).doesNotContain("https://")
        assertThat(sanitized).doesNotContain("noticias.globo.com")
        assertThat(sanitized).contains("Veja o link da internet para mais informações.")
    }

    @Test
    fun `deve remover codigos hexadecimais e sequencias de formatacao`() {
        val raw = "Erro no componente #FF00AA com código 0x1A4B."
        val sanitized = sanitizer.sanitize(raw)

        assertThat(sanitized).doesNotContain("#FF00AA")
        assertThat(sanitized).doesNotContain("0x1A4B")
    }

    @Test
    fun `deve colapsar emojis repetidos`() {
        val raw = "Parabéns Gabriel! 👏👏👏👍👍"
        val sanitized = sanitizer.sanitize(raw)

        assertThat(sanitized).doesNotContain("👏👏👏")
        assertThat(sanitized).contains("Parabéns Gabriel!")
    }
}
