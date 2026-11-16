package br.com.vozguia.core.guardrails.impl

import br.com.vozguia.core.data.repository.QuotaRepository
import br.com.vozguia.core.domain.ScreenSnapshot
import br.com.vozguia.core.domain.UserIntent
import br.com.vozguia.core.guardrails.GuardrailVerdict
import br.com.vozguia.core.guardrails.VozGuiaGuardrail

/**
 * Guardião de custos e quotas do Google AI Studio (Free Tier).
 * Impede estouro de cotas gratuitas, controla rate limit e previne consumo desnecessário de bateria.
 * Prioridade: 50.
 */
class CloudCostGuardrail(
    private val quotaRepository: QuotaRepository,
    private val maxDailyCloudCalls: Int = 50,
    private val minIntervalBetweenCallsMs: Long = 3000L,
    private val maxImageSizeBytes: Int = 4 * 1024 * 1024, // 4 MB
    private val clockProvider: () -> Long = { System.currentTimeMillis() }
) : VozGuiaGuardrail {

    override val name: String = "CloudCostGuardrail"
    override val priority: Int = 50

    override suspend fun evaluate(
        intent: UserIntent,
        currentScreen: ScreenSnapshot?
    ): GuardrailVerdict {
        // Se a intenção é resolvida localmente, custo é zero (0 tokens / R$ 0,00)
        if (intent.isLocalFastPath()) {
            return GuardrailVerdict.Allowed
        }

        // Intenções que exigem consulta à nuvem (Ex: Visão Computacional / Gemini Flash)
        val todayCalls = quotaRepository.getTodayCloudCallCount()
        if (todayCalls >= maxDailyCloudCalls) {
            return GuardrailVerdict.Blocked(
                reason = "Limite de chamadas gratuitas em nuvem atingido no dia ($todayCalls/$maxDailyCloudCalls)",
                spokenMessage = "Não consigo analisar a imagem no momento. Posso ligar para o Gabriel para você perguntar a ele?"
            )
        }

        // Throttle anti-spam / Rate limit com proteção contra manipulação de relógio
        val lastCallTime = quotaRepository.getLastCloudCallTimestamp()
        val now = clockProvider()
        val delta = now - lastCallTime
        if (lastCallTime > 0 && (delta in 0 until minIntervalBetweenCallsMs || delta < -60_000L)) {
            return GuardrailVerdict.Blocked(
                reason = "Throttle de chamadas à nuvem violado (intervalo mínimo de ${minIntervalBetweenCallsMs}ms)",
                spokenMessage = "Aguarde alguns segundos antes de pedir outra análise."
            )
        }

        // Validação de payload de imagem para economia de recursos e dados móveis
        if (intent is UserIntent.DescribeReceivedImage) {
            val imageBytes = intent.rawImageBytes
            if (imageBytes == null || imageBytes.isEmpty()) {
                return GuardrailVerdict.Blocked(
                    reason = "Tentativa de análise com payload de imagem vazio ou nulo",
                    spokenMessage = "Não foi possível carregar a imagem para descrever."
                )
            }
            if (imageBytes.size > maxImageSizeBytes) {
                return GuardrailVerdict.Blocked(
                    reason = "Tamanho da imagem excede o limite econômico (${imageBytes.size} bytes > $maxImageSizeBytes bytes)",
                    spokenMessage = "Esta imagem é muito pesada para ser analisada pela conexão atual."
                )
            }
        }

        return GuardrailVerdict.Allowed
    }
}
