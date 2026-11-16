package br.com.vozguia.core.guardrails

import br.com.vozguia.core.data.repository.QuotaRepository
import br.com.vozguia.core.guardrails.impl.AccessBoundaryGuardrail
import br.com.vozguia.core.guardrails.impl.CloudCostGuardrail
import br.com.vozguia.core.guardrails.impl.FinancialShieldGuardrail
import br.com.vozguia.core.guardrails.impl.SensitiveNodeFilter
import br.com.vozguia.core.guardrails.impl.Voice2FAGuardrail

/**
 * Fábrica oficial do pipeline completo de guardrails do Voz Guia para produção.
 * Garante que todos os 5 interceptadores de segurança, privacidade e custo Free Tier estejam ativos.
 */
object VozGuiaGuardrailFactory {

    fun createDefaultPipeline(
        quotaRepository: QuotaRepository,
        maxDailyCloudCalls: Int = 50
    ): GuardrailPipeline {
        return GuardrailPipeline(
            listOf(
                FinancialShieldGuardrail(),
                AccessBoundaryGuardrail(),
                SensitiveNodeFilter(),
                Voice2FAGuardrail(),
                CloudCostGuardrail(
                    quotaRepository = quotaRepository,
                    maxDailyCloudCalls = maxDailyCloudCalls
                )
            )
        )
    }
}
