# Guardrails do Voz Guia: Limites de Atuação, Segurança de Dados, Controle de Acessos e Engenharia de Economia Free Tier

- **Projeto:** Voz Guia — Agente Assistivo Android
- **Documento:** Especificação Técnica de Guardrails & Governança de IA (v2.0)
- **Data:** 2026-11-09
- **Status:** Aprovado para Implementação
- **Autores:** Antigravity & Gabriel
- **Hardware Homologado:** Motorola Edge 50 Neo (MediaTek Dimensity 7300, NPU APU 655, 8GB/12GB RAM, Android 14/15)
- **Premissa Econômica Fundamental:** Custo R$ 0,00 inicial via arquitetura local-first e quotas Free Tier, projetado para evolução progressiva sem aumento de custos.

---

## 1. Visão Geral e Filosofia de Governança

O **Voz Guia** opera sobre APIs de privilégios elevados do Android (`AccessibilityService`, `NotificationListenerService`, `TelecomManager`). Em um dispositivo voltado para uma pessoa com deficiência visual recente (o pai do Gabriel), o assistente precisa de autonomia para agir sem sobrecarregar o usuário, mas **jamais pode cometer ações destrutivas, expor dados privados ou gerar custos financeiros**.

A arquitetura de governança é estruturada em um **Pipeline Interceptador em 4 Estágios**, garantindo que qualquer intenção seja validada antes, durante e após o processamento:

```
[Entrada de Voz / Gatilho Físico]
               │
               ▼
┌────────────────────────────────────────────────────────┐
│  Estágio 1: Guardrail de Acessos & Permissões          │ ──► Rejeita nós de senha, apps não-autorizados
└──────────────────────────┬─────────────────────────────┘
                           │
                           ▼
┌────────────────────────────────────────────────────────┐
│  Estágio 2: Guardrail de Limites de Atuação            │ ──► Financial Shield, Bloqueio de Links, 2FA Vocal
└──────────────────────────┬─────────────────────────────┘
                           │
                           ▼
┌────────────────────────────────────────────────────────┐
│  Estágio 3: Roteador de Custos & Quota Free Tier       │ ──► Local Fast-Path (R$ 0) vs. Gemini Flash Free
└──────────────────────────┬─────────────────────────────┘
                           │
                           ▼
┌────────────────────────────────────────────────────────┐
│  Estágio 4: Guardrail de Saída & Segurança de Dados    │ ──► Sanitize PII, Áudio Volátil, TTS Seguro
└──────────────────────────┬─────────────────────────────┘
                           │
                           ▼
              [Execução Háptica & Sonora]
```

---

## 2. Pilar 1: Limitações de Uso e Limites de Atuação (Action Boundaries)

### 2.1 Allowlist Fechada de Aplicativos Interativos
O `AccessibilityService` e o `ActionDispatcher` operam sob uma política estrita de **Allowlist Negativa Fechada**. Nenhuma automação de clique, rolagem ou digitação é disparada fora dos seguintes pacotes:

| Categoria | Pacote (`packageName`) | Ações Permitidas | Ações Bloqueadas |
| :--- | :--- | :--- | :--- |
| **Mensagens** | `com.whatsapp` | Ler notificações, dar Play em áudio recebido, responder via `RemoteInput`, ditar mensagem. | Bloqueado: Excluir conversas, alterar recado, fazer compras via WhatsApp Pay, abrir links recebidos. |
| **Telefonia** | `com.google.android.dialer`, `com.android.server.telecom` | Ligar para números da agenda VIP, atender com viva-voz, encerrar chamada. | Bloqueado: Ligar para números internacionais sem confirmação, apagar histórico de chamadas. |
| **Mídia** | `com.google.android.youtube` | Pesquisa orgânica, reprodução direta, clique automático em *"Pular Anúncio"*, pausar. | Bloqueado: Inscrições em canais pagos, compras de filmes, comentários públicos, rolagem contínua em Shorts. |
| **Utilidades** | `com.google.android.deskclock`, `br.com.vozguia` | Criar e desativar alarmes de remédio, gerenciar contatos no Modo Cuidador. | Bloqueado: Desinstalação do serviço, alteração de configurações de segurança do Android. |

### 2.2 Barreira Proativa a Aplicativos Financeiros (*Financial Shield*)
Se qualquer tela de aplicativo financeiro, bancário ou governamental estiver ativa, o serviço entra imediatamente em **Modo Passivo Silencioso**:

```kotlin
val FINANCIAL_APP_SHIELD: Set<String> = setOf(
    "com.nu.production",                 // Nubank
    "br.com.bb",                         // Banco do Brasil
    "com.itau",                          // Itaú
    "br.com.bradesco",                   // Bradesco
    "br.com.santander.usuario",          // Santander
    "br.gov.caixa.tem",                  // Caixa Tem
    "br.com.intermedium",                // Banco Inter
    "com.mercadopago.wallet",            // Mercado Pago
    "com.picpay",                        // PicPay
    "com.google.android.apps.walletnfcrel", // Google Carteira
    "br.gov.meugovbr"                    // Gov.br
)
```

**Regras de Execução sob o Financial Shield:**
1. Desativação instantânea do extrator semântico de nós (`node.recycle()`).
2. Bloqueio absoluto do despachante de toques e gestos simulados (`performAction(ACTION_CLICK)` e `dispatchGesture`).
3. Resposta falada de segurança preventiva caso o usuário acione o microfone:
   > *"Por segurança, operações bancárias e transferências não podem ser feitas por voz."*

### 2.3 Proibições Absolutas (*Hard Blacklist*)
Independentemente do comando do usuário, o agente está programaticamente impedido de executar:
1. **Transações Monetárias / PIX / Compras:** Zero operações de pagamento.
2. **Exclusão de Dados Irreversível:** Proibido apagar mensagens, contatos ou fotos.
3. **Abertura de Links Externos:** Se uma mensagem contiver links web (`http://` ou `https://`), o agente lê apenas o remetente e o texto, mas **não clica no link** para evitar fraudes ou navegação desorientada.
4. **Downloads e Fontes Desconhecidas:** Proibido instalar arquivos `.apk` ou aceitar prompts de fontes externas.

### 2.4 Confirmação Vocal Obrigatória (2FA Vocal)
Ações de comunicação com o mundo externo exigem confirmação explícita de duas etapas:
- **Mensagens de WhatsApp:** O assistente transcreve, repete o conteúdo e o destinatário (*"Você disse: 'Já tomei o remédio' para Gabriel. Posso enviar?"*). O envio só ocorre após um *"Sim"* ou *"Pode enviar"*.
- **Timeout Protetivo:** Se o usuário não responder em até 5 segundos ou disser *"Não"* / *"Cancela"*, o envio é abortado com som grave de cancelamento.

---

## 3. Pilar 2: Segurança de Dados e Privacidade (Data Protection & LGPD)

### 3.1 Arquitetura 100% Local-First
- **Sem Telemetria na Nuvem:** O sistema não utiliza serviços externos de telemetria, rastreamento ou analítica (como Firebase Analytics ou Mixpanel).
- **Criptografia em Repouso:** Os dados da rotina, contatos e preferências são gravados no banco SQLite local (`Room`) e `EncryptedSharedPreferences`, criptografados via AES-256-GCM com chaves geradas no **Android Keystore** do chip MediaTek Dimensity 7300.
- **Áudio Volátil:** O microfone alimenta o STT local em buffers de memória volátil (RAM). Assim que a intenção é identificada, o buffer de áudio é zerado e liberado. **Nenhum arquivo de voz é gravado em disco.**

### 3.2 Minimização Estrita de Notificações
Em conformidade direta com a LGPD (Art. 6º, III — Princípio da Necessidade):
```kotlin
override fun onNotificationPosted(sbn: StatusBarNotification) {
    // FILTRO DE PRIVACIDADE: Descarte imediato de qualquer pacote diferente de WhatsApp
    if (sbn.packageName != "com.whatsapp") {
        return // Liberado da memória instantaneamente sem processamento ou persistência
    }
    processWhatsAppNotification(sbn)
}
```

### 3.3 Sanitização Pré-Nuvem (Zero-PII Prompts)
Nas ocasiões raras em que a nuvem gratuita for acionada (ex: descrever uma foto recebida no WhatsApp a pedido do usuário):
1. **Eliminação de Metadados:** Número de telefone, nomes de contatos, localização GPS e IDs do dispositivo são completamente removidos da requisição.
2. **Detector de Documentos Sensíveis:** Se a imagem enviada aparentar ser um documento pessoal (RG, CPF, CNH, cartão de crédito), a resposta do modelo é filtrada para alertar apenas: *"A foto parece ser um documento pessoal. Por segurança, não lerei os números em voz alta."*

---

## 4. Pilar 3: Controle de Acessos e Segregação de Privilégios

### 4.1 Separação Rigorosa de Papéis (Pai vs. Gabriel / Cuidador)

```
┌─────────────────────────────────┐       ┌─────────────────────────────────┐
│     Usuário Primário (Pai)      │       │     Administrador (Gabriel)     │
├─────────────────────────────────┤       ├─────────────────────────────────┤
│ • Interface: Botão Físico + Voz │       │ • Interface: Tela Compose       │
│ • Ações: Ligar, Ouvir, Responder│       │ • Acesso: Biometria / Senha     │
│ • Bloqueio: Sem acesso a ajustes│       │ • Ações: Contatos, Permissões,  │
│   ou configurações técnicas     │       │   Velocidade TTS, Logs de treino│
└─────────────────────────────────┘       └─────────────────────────────────┘
```

### 4.2 Proteção Biométrica do Modo Cuidador
O acesso às telas de cadastro de rotina, contatos VIP e gerenciamento de permissões exige validação via `BiometricPrompt` do Android (digital ou credencial de desbloqueio do Gabriel). Isso impede que toques involuntários alterem configurações vitais.

### 4.3 Bloqueio de Nós Sensíveis e Campos de Senha
O parser semântico de tela (`SemanticScreenTreeParser`) inspeciona os atributos de acessibilidade:
- `node.isPassword == true`: Nó imediatamente ignorado e excluído do resumo.
- `node.inputType & InputType.TYPE_TEXT_VARIATION_PASSWORD`: Filtrado.
- Teclados virtuais em primeiro plano não geram leitura de teclas digitadas para evitar vazamento de credenciais na síntese de voz.

---

## 5. Pilar 4: Engenharia de Economia Máxima e Catálogo Free Tier

### 5.1 Filosofia Econômica e Hierarquia de Custos
O sistema foi concebido para ter **custo operacional perpétuo de R$ 0,00** para a família, operando com um roteamento em 4 camadas de consumo crescente:

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                       Hierarquia de Custos do Voz Guia                      │
├─────────────────────────────────────────────────────────────────────────────┤
│ Camada 1: Fast-Path Local (Regex + Metaphone pt-BR)   ──► Custo: R$ 0,00   │
│           (Resolução em < 50ms | 0 Tokens | Offline)                        │
├─────────────────────────────────────────────────────────────────────────────┤
│ Camada 2: Motores de Áudio On-Device (Vosk + Piper)    ──► Custo: R$ 0,00   │
│           (STT e TTS 100% locais | 0 Tokens | Offline)                      │
├─────────────────────────────────────────────────────────────────────────────┤
│ Camada 3: SLM Compacto On-Device (Gemma 2B / Qwen)    ──► Custo: R$ 0,00   │
│           (NPU MediaTek APU 655 | 0 Tokens | Offline)                       │
├─────────────────────────────────────────────────────────────────────────────┤
│ Camada 4: Cloud Fallback Gratuito (Google AI Studio)   ──► Custo: R$ 0,00   │
│           (Gemini 1.5/2.0 Flash | Free Tier Oficial | 15 RPM / 1.500 RPD)   │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 5.2 Catálogo de Ferramentas e Provedores Gratuitos

| Componente | Ferramenta / Serviço Selecionado | Modelo / Versão | Custo | Justificativa Técnica |
| :--- | :--- | :--- | :--- | :--- |
| **STT (Reconhecimento)** | `Vosk Android SDK` / `Sherpa-ONNX` | Modelo pt-BR (~45MB) | **R$ 0,00** | Roda 100% offline no Dimensity 7300, baixa latência, zero dados enviados à internet. |
| **TTS (Síntese)** | `Android Neural TTS` + `Piper TTS` | Voz pt-BR quantizada int8 | **R$ 0,00** | Voz humanizada fluida, offline, sem custo de API por caractere. |
| **SLM Local (Raciocínio)** | `MediaTek NeuroPilot` / `ONNX Runtime` | `Gemma 2B` ou `Qwen 2.5 0.5B` | **R$ 0,00** | Processamento no chip APU 655 do Motorola Edge 50 Neo, sem consumir bateria excessiva. |
| **Nuvem (Visão & Fallback)** | **Google AI Studio (Gemini API)** | `gemini-1.5-flash` / `gemini-2.0-flash` | **R$ 0,00** (Free Tier) | Quota oficial gratuita sem cartão de crédito: **15 RPM**, **1.000.000 TPM**, **1.500 requisições/dia**. Suporte nativo a visão multimodal e Structured Outputs. |
| **Banco de Dados** | `Android Jetpack Room (SQLite)` | SQLite 3 Nativo | **R$ 0,00** | Sem necessidade de servidores em nuvem ou bancos pagos. |

### 5.3 Circuit Breakers e Proteções Anti-Estouro de Quota
Para garantir que a quota gratuita do Google AI Studio nunca seja excedida e para proteger a bateria contra chamadas excessivas, o cliente de nuvem aplica limites rigorosos:

```kotlin
object CloudQuotaGuardrail {
    const val MAX_CLOUD_CALLS_PER_DAY = 50          // Teto seguro por dia
    const val MAX_INPUT_TOKENS_PER_CALL = 512       // Tamanho máximo do contexto enviado
    const val MAX_OUTPUT_TOKENS_PER_CALL = 256      // Tamanho máximo da resposta sintetizada
    const val MIN_INTERVAL_BETWEEN_CALLS_MS = 3000L // Throttle anti-spam (3 segundos)

    fun canExecuteCloudCall(currentCallsToday: Int, lastCallTimestampMs: Long): Boolean {
        val now = System.currentTimeMillis()
        if (currentCallsToday >= MAX_CLOUD_CALLS_PER_DAY) return false
        if (now - lastCallTimestampMs < MIN_INTERVAL_BETWEEN_CALLS_MS) return false
        return true
    }
}
```

### 5.4 Fallback Gracioso sem Interrupção de Serviço
Se a quota diária gratuita for atingida, ou se a API retornar erro HTTP 429 (`Rate Limit Exceeded`), ou se o aparelho estiver offline:
1. O sistema **não emite mensagens de erro técnicas**.
2. O assistente informa vocalmente:
   > *"Estou operando em modo local agora. Consigo fazer ligações, ler mensagens e tocar músicas normalmente."*
3. Todas as funções essenciais (Telefone, WhatsApp áudios/mensagens e YouTube rádio) continuam operando 100% offline.

---

## 6. Arquitetura de Contratos de Código (Implementação em Kotlin)

### 6.1 Contrato Unificado de Veredito (`GuardrailVerdict`)

```kotlin
package br.com.vozguia.core.guardrails

sealed interface GuardrailVerdict {
    /** Ação permitida para execução imediata */
    object Allowed : GuardrailVerdict

    /** Ação bloqueada por motivos de segurança, cota ou política */
    data class Blocked(
        val reason: String,
        val spokenMessage: String
    ) : GuardrailVerdict

    /** Ação permitida somente mediante confirmação vocal de 2 passos */
    data class RequiresVoiceConfirmation(
        val confirmationPrompt: String,
        val actionOnConfirm: suspend () -> Unit
    ) : GuardrailVerdict
}
```

### 6.2 Interface Base de Guardrail

```kotlin
package br.com.vozguia.core.guardrails

import br.com.vozguia.core.domain.UserIntent
import br.com.vozguia.core.domain.ScreenSnapshot

interface VozGuiaGuardrail {
    val name: String
    val priority: Int

    suspend fun evaluate(
        intent: UserIntent,
        currentScreen: ScreenSnapshot?
    ): GuardrailVerdict
}
```

### 6.3 Interceptor do Financial Shield

```kotlin
package br.com.vozguia.core.guardrails.impl

import br.com.vozguia.core.guardrails.VozGuiaGuardrail
import br.com.vozguia.core.guardrails.GuardrailVerdict
import br.com.vozguia.core.domain.UserIntent
import br.com.vozguia.core.domain.ScreenSnapshot

class FinancialShieldGuardrail : VozGuiaGuardrail {
    override val name: String = "FinancialShield"
    override val priority: Int = 100 // Prioridade máxima

    private val blockedPackages = setOf(
        "com.nu.production", "br.com.bb", "com.itau", "br.com.bradesco",
        "br.com.santander.usuario", "br.gov.caixa.tem", "br.com.intermedium",
        "com.mercadopago.wallet", "com.picpay", "com.google.android.apps.walletnfcrel",
        "br.gov.meugovbr"
    )

    override suspend fun evaluate(intent: UserIntent, currentScreen: ScreenSnapshot?): GuardrailVerdict {
        val currentPackage = currentScreen?.packageName
        if (currentPackage != null && blockedPackages.contains(currentPackage)) {
            return GuardrailVerdict.Blocked(
                reason = "App financeiro detectado em primeiro plano: $currentPackage",
                spokenMessage = "Por segurança, operações bancárias e financeiras não podem ser feitas por voz."
            )
        }
        return GuardrailVerdict.Allowed
    }
}
```

### 6.4 Guardrail de Custo e Quota Free Tier

```kotlin
package br.com.vozguia.core.guardrails.impl

import br.com.vozguia.core.guardrails.VozGuiaGuardrail
import br.com.vozguia.core.guardrails.GuardrailVerdict
import br.com.vozguia.core.domain.UserIntent
import br.com.vozguia.core.domain.ScreenSnapshot
import br.com.vozguia.core.data.repository.QuotaRepository

class CloudCostGuardrail(
    private val quotaRepository: QuotaRepository
) : VozGuiaGuardrail {
    override val name: String = "CloudCostGuardrail"
    override val priority: Int = 50

    override suspend fun evaluate(intent: UserIntent, currentScreen: ScreenSnapshot?): GuardrailVerdict {
        // Se a intenção é resolvida localmente (Fast-Path), custo é 0 e sempre liberado
        if (intent.isLocalFastPath()) {
            return GuardrailVerdict.Allowed
        }

        // Se requer nuvem (ex: visão computacional de fotos recebidas)
        val dailyCalls = quotaRepository.getTodayCloudCallCount()
        if (dailyCalls >= 50) {
            return GuardrailVerdict.Blocked(
                reason = "Cota diária gratuita atingida ($dailyCalls/50)",
                spokenMessage = "Não consigo analisar a imagem no momento. Posso ligar para o Gabriel para você perguntar a ele?"
            )
        }

        return GuardrailVerdict.Allowed
    }
}
```

---

## 7. Roteiro de Evolução Futura (Mantendo Custo Mínimo / Zero)

Quando o projeto evoluir além do MVP, a estratégia de escalabilidade preservará o princípio de gratuidade e frugalidade:

1. **Aprimoramento do SLM Local:** Conforme o suporte ao MediaTek NeuroPilot amadurecer no Android 15, transferir mais comandos de linguagem natural para o `Gemma 2B / Qwen 2.5` local, reduzindo a dependência da nuvem para quase 0%.
2. **Cache Semântico Local em SQLite:** Respostas comuns para descrições de imagens ou perguntas recorrentes de rotina ficam salvas localmente, evitando qualquer rechamada de API.
3. **Serviços Gratuitos de Nuvem para Backup:** Se o Gabriel desejar sincronizar configurações de rotina entre aparelhos, utilizar o **Google Drive AppData Folder** (armazenamento gratuito na própria conta Google do usuário), sem servidores ou mensalidades.

---

## 8. Critérios de Homologação e Testes Automatizados (Gherkin)

```gherkin
Funcionalidade: Governança e Guardrails de Segurança e Custo

  Cenário: Bloqueio automático ao abrir app do Nubank
    Dado que o usuário abriu o app "com.nu.production"
    Quando o usuário apertar o botão de comando e disser "Qual é meu saldo?"
    Então o FinancialShieldGuardrail deve emitir veredito Blocked
    E o assistente deve falar: "Por segurança, operações bancárias e financeiras não podem ser feitas por voz."
    E nenhum clique deve ser disparado na tela.

  Cenário: Confirmação vocal obrigatória para envio no WhatsApp
    Dado que o usuário disse "Manda um zap pro Gabriel dizendo que estou bem"
    Quando o Roteador de Intenção classificar o envio de mensagem
    Então o veredito deve ser RequiresVoiceConfirmation com prompt "Você disse: 'Estou bem' para Gabriel. Posso enviar?"
    E a mensagem só será enviada se o usuário disser "Sim" ou "Pode enviar" em até 5 segundos.

  Cenário: Preservação de Custo Zero ao atingir teto de chamadas na nuvem
    Dado que o usuário realizou 50 consultas de visão no dia
    Quando o usuário pedir para descrever uma nova foto
    Então o CloudCostGuardrail deve bloquear a requisição externa
    E o aplicativo não deve emitir nenhum erro de API
    E o custo financeiro total deve ser rigorosamente R$ 0,00.
```
