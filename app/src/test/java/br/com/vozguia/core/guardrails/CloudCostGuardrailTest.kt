package br.com.vozguia.core.guardrails

import br.com.vozguia.core.data.repository.QuotaRepository
import br.com.vozguia.core.domain.UserIntent
import br.com.vozguia.core.guardrails.impl.CloudCostGuardrail
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class CloudCostGuardrailTest {

    private lateinit var fakeQuotaRepository: FakeQuotaRepository
    private lateinit var guardrail: CloudCostGuardrail

    @Before
    fun setUp() {
        fakeQuotaRepository = FakeQuotaRepository()
        guardrail = CloudCostGuardrail(
            quotaRepository = fakeQuotaRepository,
            maxDailyCloudCalls = 50,
            minIntervalBetweenCallsMs = 3000L,
            maxImageSizeBytes = 4 * 1024 * 1024
        )
    }

    @Test
    fun `quando intencao for Fast-Path local, libera sempre com custo zero independente de cota`() = runTest {
        fakeQuotaRepository.todayCalls = 50 // Cota esgotada

        val timeVerdict = guardrail.evaluate(UserIntent.TimeQuery, null)
        assertThat(timeVerdict).isEqualTo(GuardrailVerdict.Allowed)

        val batteryVerdict = guardrail.evaluate(UserIntent.BatteryQuery, null)
        assertThat(batteryVerdict).isEqualTo(GuardrailVerdict.Allowed)
    }

    @Test
    fun `quando intencao exigir nuvem e cota estiver dentro do limite, libera execucao`() = runTest {
        fakeQuotaRepository.todayCalls = 10
        fakeQuotaRepository.lastCallTime = 0L

        val cloudIntent = UserIntent.DescribeReceivedImage(
            rawImageBytes = ByteArray(1024)
        )

        val verdict = guardrail.evaluate(cloudIntent, null)
        assertThat(verdict).isEqualTo(GuardrailVerdict.Allowed)
    }

    @Test
    fun `quando cota diaria de nuvem for atingida, bloqueia com mensagem humana acolhedora`() = runTest {
        fakeQuotaRepository.todayCalls = 50 // Teto de 50 atingido

        val cloudIntent = UserIntent.DescribeReceivedImage(
            rawImageBytes = ByteArray(1024)
        )

        val verdict = guardrail.evaluate(cloudIntent, null)

        assertThat(verdict).isInstanceOf(GuardrailVerdict.Blocked::class.java)
        val blocked = verdict as GuardrailVerdict.Blocked
        assertThat(blocked.spokenMessage).contains("Não consigo analisar a imagem no momento. Posso ligar para o Gabriel")
        assertThat(blocked.reason).contains("50/50")
    }

    @Test
    fun `quando intervalo entre chamadas for menor que 3 segundos, bloqueia por throttle`() = runTest {
        val now = 1_000_000L
        val controlledGuardrail = CloudCostGuardrail(
            quotaRepository = fakeQuotaRepository,
            clockProvider = { now }
        )
        fakeQuotaRepository.todayCalls = 10
        fakeQuotaRepository.lastCallTime = now - 1000L // 1 segundo atrás

        val cloudIntent = UserIntent.DescribeReceivedImage(
            rawImageBytes = ByteArray(1024)
        )

        val verdict = controlledGuardrail.evaluate(cloudIntent, null)

        assertThat(verdict).isInstanceOf(GuardrailVerdict.Blocked::class.java)
        val blocked = verdict as GuardrailVerdict.Blocked
        assertThat(blocked.spokenMessage).contains("Aguarde alguns segundos")
    }

    @Test
    fun `quando imagem for vazia ou nula, bloqueia para evitar desperdicio de chamada em nuvem`() = runTest {
        val emptyIntent = UserIntent.DescribeReceivedImage(rawImageBytes = ByteArray(0))

        val verdict = guardrail.evaluate(emptyIntent, null)

        assertThat(verdict).isInstanceOf(GuardrailVerdict.Blocked::class.java)
        val blocked = verdict as GuardrailVerdict.Blocked
        assertThat(blocked.spokenMessage).contains("Não foi possível carregar a imagem")
    }

    @Test
    fun `quando imagem exceder o limite economico de 4MB, bloqueia para preservar conexao`() = runTest {
        fakeQuotaRepository.todayCalls = 5
        fakeQuotaRepository.lastCallTime = 0L

        val heavyImage = ByteArray(5 * 1024 * 1024) // 5 MB
        val cloudIntent = UserIntent.DescribeReceivedImage(rawImageBytes = heavyImage)

        val verdict = guardrail.evaluate(cloudIntent, null)

        assertThat(verdict).isInstanceOf(GuardrailVerdict.Blocked::class.java)
        val blocked = verdict as GuardrailVerdict.Blocked
        assertThat(blocked.spokenMessage).contains("Esta imagem é muito pesada")
    }

    private class FakeQuotaRepository : QuotaRepository {
        var todayCalls = 0
        var lastCallTime = 0L

        override suspend fun getTodayCloudCallCount(): Int = todayCalls
        override suspend fun getLastCloudCallTimestamp(): Long = lastCallTime
        override suspend fun recordCloudCall(timestampMs: Long) {
            todayCalls++
            lastCallTime = timestampMs
        }
        override suspend fun resetDailyCount() {
            todayCalls = 0
        }
    }
}
