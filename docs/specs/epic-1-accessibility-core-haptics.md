# Épico 1: Core de Acessibilidade & Gatilhos Físicos — Especificação Funcional

- **Projeto:** Voz Guia — Agente Assistivo Android
- **Épico:** 1 de 4
- **Status:** Especificação Pronta para Implementação
- **Componente Principal:** `VozGuiaAccessibilityService` & `HapticFeedbackController`

---

## 1. Objetivo do Épico

Substituir a necessidade de gestos complexos e toques duplos na tela por um **serviço nativo Android de acessibilidade** capaz de:
1. Interceptar comandos através de **botões físicos** (segurar Volume Menos) e toques amplos com a palma da mão na tela.
2. Fornecer **Feedback Háptico Imediato (< 50ms)**: ao menor sinal de toque ou comando, vibrar suavemente e tocar um som afirmativo sutil, eliminando o "vazio no escuro".
3. Ler a estrutura da tela ativa de qualquer aplicativo (`AccessibilityNodeInfo`) de forma filtrada (ignorando botões vazios e ruídos estruturais).
4. Despachar cliques e ações programáticas de navegação (`ACTION_CLICK`, `ACTION_SCROLL_FORWARD`, `GLOBAL_ACTION_BACK`, `GLOBAL_ACTION_HOME`).

---

## 2. Requisitos Funcionais (RF)

- **RF-1.1 (Registro do AccessibilityService):** O app deve declarar e configurar um `AccessibilityService` nativo com as seguintes flags essenciais no arquivo de configuração XML (`res/xml/accessibility_service_config.xml`):
  - `accessibilityEventTypes="typeAllMask"`
  - `accessibilityFeedbackType="feedbackSpoken|feedbackHaptic"`
  - `canRetrieveWindowContent="true"`
  - `canRequestFilterKeyEvents="true"`
  - `canPerformGestures="true"`
- **RF-1.2 (Interceptação Inteligente de Botões Físicos):**
  - Implementar no método `onKeyEvent(event: KeyEvent)` a detecção de **Pressão Longa (Hold de 1.000ms) no botão de Volume para Baixo (`KEYCODE_VOLUME_DOWN`)**.
  - Toques rápidos normais no volume continuam alterando o volume do aparelho normalmente (sem travar o áudio do usuário).
  - A pressão longa consome o evento (`return true`) e aciona imediatamente o estado de **"Escuta Ativa do Agente"**.
- **RF-1.3 (Gatilho de Tela por Palma/Toque Amplo):**
  - Uma camada de sobreposição transparente acessível (Overlay ou tela de toque ativo) onde um toque com área de contato ampla (detectando múltiplos pontos ou área de toque maior que 40% da tela) ativa a escuta do agente sem precisar de mira pontual.
- **RF-1.4 (Controlador de Feedback Háptico e Sonoro Imediato):**
  - `HapticFeedbackController` executado em thread de alta prioridade.
  - Ao iniciar escuta: dispara padrão de vibração tátil suave (`VibrationEffect.createPrebaked(EFFECT_CLICK)` ou waveform de 40ms) + som afirmativo agradável (`ToneGenerator` ou áudio de 50ms pré-carregado na memória). Latência máxima: **< 40ms**.
  - Enquanto o processamento estiver ocorrendo (se passar de 1s): emite um pulso tátil rítmico suave a cada 1 segundo para manter o usuário consciente de que o aparelho está trabalhando.
  - Ao concluir ou responder: vibração dupla curta (`tick-tick`).
- **RF-1.5 (Extrator Semântico da Árvore de UI):**
  - O método `parseActiveWindow(rootNode: AccessibilityNodeInfo): ScreenContentSummary` deve percorrer a árvore de nós recursivamente, coletando apenas nós que possuam:
    - Texto visível (`text != null`) ou descrição de conteúdo acessível (`contentDescription != null`).
    - Ignorar nós vazios, layouts de preenchimento (`LinearLayout`, `ViewGroup` sem conteúdo próprio) e nós não clicáveis sem texto.
    - Identificar nós de ação rápida: botões de play de áudio, campo de digitação de mensagem e botões de chamada.

---

## 3. Critérios de Aceite (Definition of Done)

- [ ] **CA-1.1:** O serviço aparece nas configurações de Acessibilidade do Android com o nome "Voz Guia" e pode ser ativado com sucesso.
- [ ] **CA-1.2:** Segurar o botão de Volume Menos por 1 segundo dispara a vibração e o som de confirmação em menos de 50 milissegundos, comprovado por logs com timestamp.
- [ ] **CA-1.3:** Pressionar rapidamente o volume aumenta/diminui o volume normalmente sem disparar o agente.
- [ ] **CA-1.4:** Ao abrir uma conversa de teste no WhatsApp, o `parseActiveWindow` extrai corretamente o nome do contato e as últimas mensagens de texto/áudio sem poluição de nós vazios.
