package br.com.vozguia.core.guardrails

import br.com.vozguia.core.domain.ScreenSnapshot
import br.com.vozguia.core.domain.SemanticElement
import br.com.vozguia.core.domain.UserIntent
import br.com.vozguia.core.guardrails.impl.SensitiveNodeFilter
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class SensitiveNodeFilterTest {

    private lateinit var filter: SensitiveNodeFilter

    @Before
    fun setUp() {
        filter = SensitiveNodeFilter()
    }

    @Test
    fun `sanitizeElements deve remover elementos marcados com isPassword ou texto de senha`() {
        val elements = listOf(
            SemanticElement(text = "Nome do Usuário", isClickable = false),
            SemanticElement(text = "Digite sua senha", isPassword = true),
            SemanticElement(contentDescription = "Campo de senha secreta", isClickable = true),
            SemanticElement(text = "Entrar", isClickable = true)
        )

        val sanitized = filter.sanitizeElements(elements)

        assertThat(sanitized).hasSize(2)
        assertThat(sanitized.map { it.text }).containsExactly("Nome do Usuário", "Entrar")
    }

    @Test
    fun `quando tela contiver campos de senha ativos, bloqueia interacao de automacao de tela em nós sensiveis`() = runTest {
        val screenWithPassword = ScreenSnapshot(
            packageName = "com.exemplo.app",
            hasPasswordFields = true,
            actionableElements = listOf(
                SemanticElement(id = "pwd_input", isPassword = true, isClickable = true)
            )
        )

        val verdict = filter.evaluate(UserIntent.YouTubeSkipAd, screenWithPassword)

        assertThat(verdict).isInstanceOf(GuardrailVerdict.Blocked::class.java)
        val blocked = verdict as GuardrailVerdict.Blocked
        assertThat(blocked.spokenMessage).contains("campos de senha não podem ser operados por comando de voz")
    }

    @Test
    fun `em tela com senha, comandos vitais globais como horas, bateria, cancelar e socorro continuam liberados`() = runTest {
        val screenWithPassword = ScreenSnapshot(
            packageName = "com.exemplo.app",
            hasPasswordFields = true,
            actionableElements = listOf(
                SemanticElement(id = "pwd_input", isPassword = true, isClickable = true)
            )
        )

        assertThat(filter.evaluate(UserIntent.TimeQuery, screenWithPassword)).isEqualTo(GuardrailVerdict.Allowed)
        assertThat(filter.evaluate(UserIntent.DateQuery, screenWithPassword)).isEqualTo(GuardrailVerdict.Allowed)
        assertThat(filter.evaluate(UserIntent.BatteryQuery, screenWithPassword)).isEqualTo(GuardrailVerdict.Allowed)
        assertThat(filter.evaluate(UserIntent.StopOrCancel, screenWithPassword)).isEqualTo(GuardrailVerdict.Allowed)
        assertThat(filter.evaluate(UserIntent.DirectCall("Gabriel"), screenWithPassword)).isEqualTo(GuardrailVerdict.Allowed)
        assertThat(filter.evaluate(UserIntent.EmergencySos, screenWithPassword)).isEqualTo(GuardrailVerdict.Allowed)
        assertThat(filter.evaluate(UserIntent.DescribeScreenContext, screenWithPassword)).isEqualTo(GuardrailVerdict.Allowed)
        assertThat(filter.evaluate(UserIntent.SleepMode, screenWithPassword)).isEqualTo(GuardrailVerdict.Allowed)
    }
}
