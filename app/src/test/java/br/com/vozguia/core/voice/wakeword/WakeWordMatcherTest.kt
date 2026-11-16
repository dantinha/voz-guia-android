package br.com.vozguia.core.voice.wakeword

import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

class WakeWordMatcherTest {

    private lateinit var matcher: WakeWordMatcher
    private val defaultConfig = VoiceAssistantConfig(
        wakePhrase = "Voz Guia",
        customAliases = listOf("Oi Guia", "Ei Guia", "Voz Guia")
    )

    @Before
    fun setUp() {
        matcher = WakeWordMatcher()
    }

    @Test
    fun `detecta frase de ativacao isolada com ou sem saudacao`() {
        val hits = listOf("Voz Guia", "oi voz guia", "Ei Guia!", "Olá Voz Guia", "ok oi guia")
        for (phrase in hits) {
            val result = matcher.match(phrase, defaultConfig)
            assertThat(result.detected).isTrue()
            assertThat(result.remainingCommand).isEmpty()
        }
    }

    @Test
    fun `extrai comando restante quando usuario fala wake word e comando na mesma frase`() {
        val result = matcher.match("Oi Voz Guia, ligar para o Gabriel", defaultConfig)
        assertThat(result.detected).isTrue()
        assertThat(result.remainingCommand).isEqualTo("ligar para o Gabriel")
    }

    @Test
    fun `respeita nome personalizado configurado pelo usuario`() {
        val customConfig = VoiceAssistantConfig(
            wakePhrase = "Bento",
            customAliases = listOf("Seu Bento", "Bento")
        )

        val directHit = matcher.match("Ei Bento, que horas são?", customConfig)
        assertThat(directHit.detected).isTrue()
        assertThat(directHit.remainingCommand).isEqualTo("que horas são?")

        val oldWakeIgnored = matcher.match("Voz Guia que horas são", customConfig)
        assertThat(oldWakeIgnored.detected).isFalse()
    }

    @Test
    fun `aceita pequena variacao fonetica ou de transcricao offline`() {
        val result = matcher.match("Vós guia, toca música no YouTube", defaultConfig)
        assertThat(result.detected).isTrue()
        assertThat(result.remainingCommand).isEqualTo("toca música no YouTube")
    }

    @Test
    fun `nao dispara falso positivo com fala cotidiana sem palavra de ativacao`() {
        val falseCandidates = listOf(
            "bom dia como voce esta hoje",
            "quero tomar cafe da manha",
            "desliga a televisao da sala",
            "oi tudo bem com voce"
        )
        for (phrase in falseCandidates) {
            val result = matcher.match(phrase, defaultConfig)
            assertThat(result.detected).isFalse()
        }
    }

    @Test
    fun `detecta wake word usando lista de multiplas hipoteses do STT`() {
        val result = matcher.matchCandidates(
            hypotheses = listOf("vós via ligar pro gabriel", "voz guia ligar pro gabriel"),
            config = defaultConfig
        )
        assertThat(result.detected).isTrue()
        assertThat(result.remainingCommand).isEqualTo("ligar pro gabriel")
    }
}
