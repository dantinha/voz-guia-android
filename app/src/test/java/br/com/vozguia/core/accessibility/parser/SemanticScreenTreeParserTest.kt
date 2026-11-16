package br.com.vozguia.core.accessibility.parser

import br.com.vozguia.core.domain.ScreenRect
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

class SemanticScreenTreeParserTest {

    private lateinit var parser: SemanticScreenTreeParser

    @Before
    fun setUp() {
        parser = SemanticScreenTreeParser(
            maxDepth = 18,
            maxNodes = 250
        )
    }

    @Test
    fun `deve descartar nos invisiveis e containers estruturais vazios`() {
        val root = MockNode(
            className = "android.widget.FrameLayout",
            isVisibleToUser = true,
            children = listOf(
                // Container invisível (deve ser descartado junto com seus filhos)
                MockNode(
                    text = "Texto Invisivel",
                    isVisibleToUser = false
                ),
                // Container vazio sem texto nem descrição (deve ser ignorado)
                MockNode(
                    className = "android.widget.LinearLayout",
                    isVisibleToUser = true,
                    text = null,
                    contentDescription = null
                ),
                // Elemento visível com texto legítimo
                MockNode(
                    className = "android.widget.TextView",
                    isVisibleToUser = true,
                    text = "Gabriel: Já almoçou, pai?",
                    isClickable = false,
                    bounds = ScreenRect(0, 100, 500, 200)
                ),
                // Botão de áudio clicável
                MockNode(
                    className = "android.widget.ImageView",
                    isVisibleToUser = true,
                    contentDescription = "Reproduzir mensagem de voz",
                    isClickable = true,
                    bounds = ScreenRect(50, 250, 150, 350)
                )
            )
        )

        val snapshot = parser.parse(root, packageName = "com.whatsapp")

        assertThat(snapshot.actionableElements).hasSize(2)
        assertThat(snapshot.actionableElements[0].text).isEqualTo("Gabriel: Já almoçou, pai?")
        assertThat(snapshot.actionableElements[1].contentDescription).isEqualTo("Reproduzir mensagem de voz")
        assertThat(snapshot.actionableElements[1].isClickable).isTrue()
    }

    @Test
    fun `deve identificar presenca de campos de senha`() {
        val root = MockNode(
            className = "android.widget.LinearLayout",
            isVisibleToUser = true,
            children = listOf(
                MockNode(
                    className = "android.widget.EditText",
                    isVisibleToUser = true,
                    isPassword = true
                )
            )
        )

        val snapshot = parser.parse(root, packageName = "com.exemplo.login")

        assertThat(snapshot.hasPasswordFields).isTrue()
    }

    @Test
    fun `deve respeitar limites maximos de profundidade e quantidade de nos`() {
        // Árvore com mais de 300 nós aninhados
        var current = MockNode(text = "Nível 0", isVisibleToUser = true)
        val root = current
        for (i in 1..300) {
            val child = MockNode(text = "Nível $i", isVisibleToUser = true)
            current.children = listOf(child)
            current = child
        }

        val snapshot = parser.parse(root, packageName = "com.whatsapp")

        // Não deve estourar a pilha e deve limitar a profundidade
        assertThat(snapshot.actionableElements.size).isAtMost(18)
    }

    @Test
    fun `deve preservar viewIdResourceName em SemanticElement_id para matching resiliente`() {
        val root = MockNode(
            className = "android.widget.FrameLayout",
            isVisibleToUser = true,
            children = listOf(
                MockNode(
                    viewIdResourceName = "com.whatsapp:id/send",
                    contentDescription = "Enviar",
                    isVisibleToUser = true,
                    isClickable = true
                )
            )
        )

        val snapshot = parser.parse(root, packageName = "com.whatsapp")

        assertThat(snapshot.actionableElements).hasSize(1)
        assertThat(snapshot.actionableElements[0].id).isEqualTo("com.whatsapp:id/send")
    }

    private class MockNode(
        override val text: CharSequence? = null,
        override val contentDescription: CharSequence? = null,
        override val className: CharSequence? = null,
        override val packageName: CharSequence? = null,
        override val viewIdResourceName: String? = null,
        override val isVisibleToUser: Boolean = true,
        override val isClickable: Boolean = false,
        override val isPassword: Boolean = false,
        override val bounds: ScreenRect = ScreenRect(0, 0, 100, 100),
        var children: List<AccessibleNode> = emptyList()
    ) : AccessibleNode {
        override val childCount: Int get() = children.size
        override fun getChild(index: Int): AccessibleNode? = children.getOrNull(index)
    }
}
