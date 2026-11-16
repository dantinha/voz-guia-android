package br.com.vozguia.core.accessibility.matcher

import br.com.vozguia.core.domain.ScreenRect
import br.com.vozguia.core.domain.SemanticElement
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

class SemanticNodeMatcherTest {

    private lateinit var matcher: SemanticNodeMatcher

    @Before
    fun setUp() {
        matcher = SemanticNodeMatcher()
    }

    @Test
    fun `deve encontrar elemento por viewId em primeira tentativa (Passo 1)`() {
        val elements = listOf(
            SemanticElement(id = "com.whatsapp:id/send", isClickable = true),
            SemanticElement(id = "com.whatsapp:id/entry", isClickable = true)
        )

        val criteria = MatchCriteria(
            idSuffix = "send",
            textOrDescKeywords = listOf("enviar")
        )

        val found = matcher.findFirstMatching(elements, criteria)

        assertThat(found).isNotNull()
        assertThat(found?.id).isEqualTo("com.whatsapp:id/send")
    }

    @Test
    fun `quando viewId for ofuscado, deve encontrar por contentDescription ou texto (Passo 2 - Fallback RN-3_02)`() {
        val elements = listOf(
            // ID ofuscado por atualização do WhatsApp (ex: "com.whatsapp:id/a1b2c")
            SemanticElement(
                id = "com.whatsapp:id/a1b2c",
                contentDescription = "Enviar",
                isClickable = true,
                bounds = ScreenRect(950, 1800, 1050, 1900)
            )
        )

        val criteria = MatchCriteria(
            idSuffix = "send", // ID antigo que não existe mais
            textOrDescKeywords = listOf("enviar", "send")
        )

        val found = matcher.findFirstMatching(elements, criteria)

        assertThat(found).isNotNull()
        assertThat(found?.contentDescription).isEqualTo("Enviar")
    }

    @Test
    fun `quando ID e texto mudarem, deve localizar por posicao relativa (Passo 3 - Canto Inferior Direito)`() {
        val elements = listOf(
            // Ícone sem texto no canto inferior direito da tela (típico do botão de enviar)
            SemanticElement(
                isClickable = true,
                bounds = ScreenRect(950, 1850, 1050, 1950)
            ),
            SemanticElement(
                isClickable = true,
                bounds = ScreenRect(100, 100, 200, 200)
            )
        )

        val criteria = MatchCriteria(
            idSuffix = "send",
            textOrDescKeywords = listOf("enviar"),
            positionFilter = { bounds -> bounds.right > 900 && bounds.bottom > 1800 }
        )

        val found = matcher.findFirstMatching(elements, criteria)

        assertThat(found).isNotNull()
        assertThat(found?.bounds?.right).isEqualTo(1050)
    }
}
