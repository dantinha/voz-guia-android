# Épico 1: Core de Acessibilidade, Gatilhos Físicos & Escudo de Tela — Especificação Funcional Detalhada

- **Projeto:** Voz Guia — Agente Assistivo Android
- **Épico:** 1 de 5
- **Versão:** 2.0 (Senior PM Review)
- **Prioridade:** P0 (Fundação Crítica do Produto)
- **Componentes de Arquitetura:** `VozGuiaAccessibilityService`, `PhysicalTriggerInterceptor`, `HapticAudioFeedbackEngine`, `SemanticScreenTreeParser`, `TouchShieldOverlayController`

---

## 1. Visão Geral e Valor de Produto do Épico

Para uma pessoa que perdeu a visão recentemente, a tela sensível ao toque deixa de ser um mapa visual e vira um campo minado: qualquer encostar de dedo nas bordas curvas do **Motorola Edge 50 Neo** clica em botões invisíveis, fecha aplicativos ou abre anúncios, enquanto acertar o "toque duplo" do TalkBack gera frustração constante.

O **Épico 1** substitui a interação visual-espacial por uma **interação física, tátil e à prova de erros**:
1. O usuário comanda o celular usando **botões físicos táteis na lateral do aparelho** (ou a palma inteira da mão).
2. Cada ação física produz uma **resposta vibratória e sonora em menos de 50ms**, eliminando para sempre a sensação de "ficar no escuro".
3. Um **Escudo de Toque (*Touch Shield*)** protege as bordas e a tela contra esbarrões acidentais quando o usuário está apenas segurando o telefone.

---

## 2. Decomposição em Features e User Stories

### FEAT-1.1: Serviço Nativo `AccessibilityService` & Extrator Semântico de UI
Motor de baixo nível registrado no Android 14/15 capaz de ler a janela ativa de qualquer aplicativo, limpar a poluição visual e despachar cliques, textos e gestos.

#### US-1.1.1: Leitura Limpa e Semântica da Tela Ativa
- **Como** usuário cego com o celular aberto em um aplicativo,
- **Quero** que o agente entenda qual aplicativo está aberto e quais são as informações e botões realmente úteis na tela, ignorando ícones decorativos, layouts vazios e propagandas,
- **Para que** eu nunca precise ouvir descrições técnicas ou robóticas da estrutura da tela.
- **Critérios de Aceite (Gherkin):**
  - **Dado** que o `VozGuiaAccessibilityService` está ativo e uma janela está em primeiro plano (`rootInActiveWindow != null`),
  - **Quando** o `SemanticScreenTreeParser` varrer a árvore de `AccessibilityNodeInfo`,
  - **Então** ele deve descartar nós invisíveis (`!isVisibleToUser`), containers estruturais vazios (`FrameLayout`, `LinearLayout` sem texto nem `contentDescription`) e elementos decorativos,
  - **E** deve retornar um objeto `ScreenSnapshot` contendo: `packageName`, `appTitle`, `screenType` (ex: `WHATSAPP_CHAT_LIST`, `WHATSAPP_CONVERSATION`, `YOUTUBE_PLAYER`, `INCOMING_CALL`), lista ordenada de `SemanticElements` (textos lidos) e mapa de `ActionableTargets` (botões clicáveis com suas coordenadas e ações).

#### US-1.1.2: Despacho Programático Confiável de Cliques e Gestos
- **Como** agente assistivo executando uma ordem do usuário,
- **Quero** clicar em elementos da tela por referência de nó (`ACTION_CLICK`) ou, quando o aplicativo não suportar clique semântico, por injeção de gesto nas coordenadas (`dispatchGesture`),
- **Para que** o usuário nunca precise procurar ou tocar fisicamente no botão na tela.
- **Critérios de Aceite (Gherkin):**
  - **Dado** um `ActionableTarget` identificado na árvore de acessibilidade,
  - **Quando** o `ActionDispatcher` tentar acioná-lo,
  - **Então** deve primeiro executar `node.performAction(ACTION_CLICK)` no nó (ou no seu ancestral clicável imediato),
  - **E** caso retorne `false`, deve calcular o centro do `Rect` (`getBoundsInScreen`) e disparar um `GestureDescription` de toque de `80ms` nas coordenadas `(centerX, centerY)`.

---

### FEAT-1.2: Gatilho Físico Sem Mira (`Hold Volume Down` & Despertar de Tela)
Elimina o toque duplo. O usuário aciona o assistente segurando o botão físico de **Volume para Baixo** por `900ms`, mantendo os toques rápidos livres para aumentar/diminuir o som normalmente.

#### US-1.2.1: Acionamento da Escuta por Pressão Longa no Volume Menos
- **Como** usuário cego segurando o Motorola Edge 50 Neo,
- **Quero** segurar o botão físico de diminuir volume por cerca de 1 segundo para abrir o microfone do agente, mantendo os cliques rápidos normais para controlar o volume,
- **Para que** eu consiga falar com o celular de olhos fechados, sem precisar olhar ou tocar na tela.
- **Critérios de Aceite (Gherkin):**
  - **Dado** que a tela do aparelho está acesa e `onKeyEvent` recebe `KEYCODE_VOLUME_DOWN` com `ACTION_DOWN`,
  - **Quando** o usuário soltar o botão em menos de `500ms` (`Tap Curto`),
  - **Então** o serviço ajusta o volume de mídia/chamada normalmente em `-1 step` via `AudioManager.adjustVolume` (ou quando em estado `SPEAKING`, executa o comando de *Cala a Boca / Cancelar*),
  - **Mas quando** o usuário mantiver o botão pressionado por `900ms` (`Hold Longo`),
  - **Então** o timer dispara imediatamente o estado `LISTENING`, sem diminuir o volume do aparelho.

#### US-1.2.2: Guia Auditivo Imediato ao Acender a Tela (Wake-Up Orientation)
- **Como** usuário cego que acabou de apertar o botão Power para tirar o celular do repouso,
- **Quero** ouvir imediatamente um sinal suave com a hora atual e o aviso se há chamadas perdidas ou mensagens novas,
- **Para que** eu saiba que a tela acendeu e que o botão de comando já está pronto para uso.
- **Critérios de Aceite (Gherkin):**
  - **Dado** que o aparelho estava com a tela apagada (`Intent.ACTION_SCREEN_OFF`),
  - **Quando** a tela acender (`Intent.ACTION_SCREEN_ON`),
  - **Então** em menos de `300ms` o agente emite uma vibração curta de despertar e diz de forma enxuta: *"Tela ligada, [HH] horas e [MM] minutos"* (acrescentando *"Tem [N] mensagens novas"* apenas se houver notificações pendentes).

---

### FEAT-1.3: Motor Háptico/Sonoro `< 50ms` & *Heartbeat* de Processamento
Garante o princípio **P-01 (Zero Silêncio no Escuro)**. O usuário sente na mão e ouve no ouvido cada mudança de estado do agente.

#### US-1.3.1: Assinaturas Táteis e Sonoras Distintas por Estado (< 50ms)
- **Como** usuário cego interagindo por voz,
- **Quero** sentir vibrações e ouvir bips musicais diferentes para *"Comecei a te ouvir"*, *"Estou pensando"*, *"Deu certo"* e *"Cancelei"*,
- **Para que** eu saiba exatamente o que o celular está fazendo em cada segundo.
- **Tabela Oficial de Assinaturas Hápticas e Sonoras (Requisito Obrigatório):**

| Evento | Latência Alvo | Padrão de Vibração (`VibrationEffect`) | Sinal Sonoro (`SoundPool` Pré-carregado) |
| :--- | :--- | :--- | :--- |
| **1. Microfone Aberto (`LISTENING`)** | `< 40ms` | Pulso único firme e curto (`50ms`, amplitude `180`) | Bip duplo ascendente suave (`Dó5 → Sol5`) |
| **2. Fim da Fala Captada (`THINKING`)** | `< 40ms` | Micro-clique suave (`25ms`, amplitude `100`) | Clique acústico aveludado |
| **3. Processando > 1s (`HEARTBEAT`)** | A cada `1000ms` | Pulso leve tipo batida de relógio (`15ms`, amplitude `60`) | Tick de madeira sutil (volume 20%) |
| **4. Ação Concluída (`SUCCESS`)** | `< 40ms` | Vibração dupla alegre (`40ms - pausa 60ms - 60ms`) | Acorde curto de confirmação |
| **5. Cancelado / Parar (`CANCELLED`)** | `< 30ms` | Pulso grave descendente (`80ms`) | Bip descendente suave (`Sol4 → Dó4`) |

---

### FEAT-1.4: Botão Universal de Pânico e Silêncio (*Panic Cancel*)
Qualquer pessoa aprendendo a usar um assistente precisa de um "freio de mão" infalível: se o celular começar a falar demais, tocar o vídeo errado ou se o usuário mudar de ideia, um único aperto resolve tudo.

#### US-1.4.1: Interrupção Instantânea de Fala ou Ação em Andamento
- **Como** usuário cego que quer fazer o celular parar de falar ou cancelar um comando imediatamente,
- **Quero** dar **1 clique curto no botão de Volume Menos** (ou falar *"Para"* / *"Cancela"*),
- **Para que** o assistente se cale na mesma fração de segundo e volte ao estado de repouso.
- **Critérios de Aceite (Gherkin):**
  - **Dado** que o agente está no estado `SPEAKING`, `LISTENING`, `THINKING` ou `AWAITING_CONFIRMATION`,
  - **Quando** o usuário der 1 toque curto em `KEYCODE_VOLUME_DOWN`,
  - **Então** em menos de `50ms` o TTS é interrompido (`tts.stop()`), qualquer fluxo pendente de envio de mensagem ou discagem é abortado, o sinal háptico `CANCELLED` é emitido e o estado retorna para `IDLE`.

---

### FEAT-1.5: Escudo de Tela Contra Toques Acidentais (*Touch Shield*) e Gesto de Palma
Protege o usuário contra o problema clássico das telas borda-a-borda do Motorola Edge 50 Neo: esbarrar os dedos na tela enquanto segura o aparelho.

#### US-1.5.1: Modo Escudo de Tela com Gesto Amplo de Palma
- **Como** usuário cego segurando o celular na mão,
- **Quero** que toques involuntários de um dedo nas bordas ou no meio da tela não cliquem em botões aleatórios dos aplicativos, mas que encostar a palma da mão inteira na tela funcione como um botão gigante para chamar o assistente,
- **Para que** eu possa segurar o telefone com tranquilidade sem medo de bagunçar a tela ou sair do que estou ouvindo.
- **Critérios de Aceite (Gherkin):**
  - **Dado** que o **Modo Escudo de Tela** está ativado,
  - **Quando** o usuário encostar acidentalmente 1 dedo nas bordas da tela,
  - **Então** o toque é absorvido pelo escudo sem disparar cliques no aplicativo abaixo,
  - **Quando** o usuário pousar a palma da mão (ou 3+ dedos simultâneos) na tela por `500ms`,
  - **Então** o agente aciona o estado `LISTENING` exatamente como no botão físico de volume,
  - **E quando** o familiar vidente (Gabriel) quiser usar a tela normalmente, basta tocar no botão discreto **"Pausar Escudo por 15 min"** no topo da tela (com proteção de duplo clique no ícone do cadeado).

---

## 3. Regras de Negócio e Casos de Borda do Épico 1

- **RN-1.01 (Convivência com Volume de Mídia durante o YouTube/WhatsApp):** Quando um áudio do WhatsApp ou vídeo do YouTube estiver tocando e o agente estiver em `IDLE`, toques curtos nos botões `Volume Mais` e `Volume Menos` ajustam o volume da mídia normalmente. Apenas o **Hold Longo (`900ms`) no Volume Menos** chama o agente.
- **RN-1.02 (Proteção de Memória no Parser de Árvore):** O Android limita a profundidade e exige reciclagem ou GC de nós antigos. O `SemanticScreenTreeParser` deve limitar a busca em profundidade (DFS) a no máximo `depth = 18` e `maxNodes = 250`, garantindo tempo de parse `< 35ms`.
- **RN-1.03 (Auto-Recuperação de Foco de Janela):** Se um popup do sistema (ex: diálogo de permissão ou alerta de bateria do Android) aparecer por cima do WhatsApp, o parser deve detectar a mudança de `packageName` (`com.android.systemui`) e avisar o usuário ou dispensar diálogos não-críticos.
