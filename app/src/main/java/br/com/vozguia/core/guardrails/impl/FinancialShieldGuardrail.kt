package br.com.vozguia.core.guardrails.impl

import br.com.vozguia.core.domain.ScreenSnapshot
import br.com.vozguia.core.domain.UserIntent
import br.com.vozguia.core.guardrails.GuardrailVerdict
import br.com.vozguia.core.guardrails.VozGuiaGuardrail

/**
 * Barreira de proteção máxima contra interação com aplicativos bancários, carteiras digitais e serviços governamentais.
 * Prioridade: 100 (Mais alta do sistema).
 */
class FinancialShieldGuardrail(
    customShieldedPackages: Set<String> = emptySet()
) : VozGuiaGuardrail {

    override val name: String = "FinancialShieldGuardrail"
    override val priority: Int = 100

    private val defaultShieldedPackages = setOf(
        "com.nu.production",                    // Nubank
        "br.com.bb",                            // Banco do Brasil
        "com.itau",                             // Itaú
        "br.com.bradesco",                      // Bradesco
        "br.com.santander.usuario",             // Santander
        "br.gov.caixa.tem",                     // Caixa Tem
        "br.gov.caixa.fgts",                    // FGTS Caixa
        "br.com.gabba.Caixa",                   // Caixa Econômica Federal (App Principal)
        "br.com.intermedium",                   // Banco Inter
        "com.c6bank.app",                       // C6 Bank
        "br.com.bancopan",                      // Banco Pan
        "br.com.neon",                          // Neon
        "br.com.uol.ps",                        // PagBank
        "br.com.uol.ps.myaccount",              // PagBank Conta
        "com.mercadopago.wallet",               // Mercado Pago
        "com.picpay",                           // PicPay
        "com.google.android.apps.walletnfcrel", // Google Carteira
        "br.com.xp.wallet",                     // XP Investimentos
        "br.com.xp.investimentos",              // XP Investimentos App
        "br.com.rico",                          // Rico
        "com.btg.pactual.banking",              // BTG Pactual
        "br.com.clear",                         // Clear Corretora
        "br.com.sicoobnet",                     // Sicoob
        "br.com.sicredi.mobile",                // Sicredi
        "br.com.safra.bancosafra",              // Safra
        "br.gov.meugovbr"                       // Gov.br
    )

    private val shieldedPackages: Set<String> = defaultShieldedPackages + customShieldedPackages

    override suspend fun evaluate(
        intent: UserIntent,
        currentScreen: ScreenSnapshot?
    ): GuardrailVerdict {
        // Emergência médica ou física NUNCA pode ser bloqueada, mesmo com app bancário na tela
        if (intent is UserIntent.EmergencySos) {
            return GuardrailVerdict.Allowed
        }

        val currentPackage = currentScreen?.packageName

        if (currentPackage != null && shieldedPackages.contains(currentPackage)) {
            return GuardrailVerdict.Blocked(
                reason = "Aplicativo financeiro/bancário detectado em primeiro plano: $currentPackage",
                spokenMessage = "Por segurança, operações bancárias e transferências não podem ser feitas por voz."
            )
        }

        return GuardrailVerdict.Allowed
    }
}
