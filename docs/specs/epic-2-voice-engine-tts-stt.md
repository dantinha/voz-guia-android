# Épico 2: Motor de Voz Humanizada, Áudio & Inteligência Híbrida — Especificação Funcional Detalhada

- **Projeto:** Voz Guia — Agente Assistivo Android
- **Épico:** 2 de 5
- **Versão:** 2.0 (Senior PM Review)
- **Prioridade:** P0 (Cérebro Conversacional e Pipeline de Áudio)
- **Componentes de Arquitetura:** `HumanVoiceTtsEngine`, `OfflineSttRecognizer`, `AudioFocusCoordinator`, `HybridIntentRouter`, `DialogueStateManager`

---

## 1. Visão Geral e Valor de Produto do Épico

A voz é a única interface que o pai do Gabriel enxerga. Se ela for apressada, metálica, falar termos técnicos ou atropelar o som do WhatsApp/YouTube, a experiência falha. Mais ainda: se todo comando simples (*"Para"*, *"Que horas são?"*, *"Toca"*) precisar ir até um servidor na internet ou rodar um LLM pesado de vários segundos, o celular parecerá lento e travado.

O **Épico 2** constrói um sistema de áudio e inteligência em **3 camadas de velocidade**:
1. **Camada 1 — Fast-Path Determinístico Local (`< 50ms`):** Comandos diretos e confirmações (*"Sim"*, *"Não"*, *"Para"*, *"Pausa"*, *"Que horas são?"*, *"Bateria"*, *"Repete"*) são resolvidos instantaneamente na CPU sem esperar LLM.
2. **Camada 2 — Inteligência On-Device (Gemma 2B / Qwen 1.5B na NPU Dimensity 7300):** Compreensão de frases naturais offline, extração de parâmetros (nome do contato, busca musical) e resumo enxuto de telas/mensagens.
3. **Camada 3 — Cloud Fallback (Gemini Flash quando houver Wi-Fi/4G):** Raciocínio aberto complexo, perguntas gerais de conhecimento ou descrição visual de fotos recebidas no WhatsApp.

---

## 2. Decomposição em Features e User Stories

### FEAT-2.1: Síntese de Voz Neural Humanizada pt-BR com *Barge-in*
Substitui o som robótico do TalkBack por uma voz brasileira natural, calma, com pausas respiratórias entre frases e corte instantâneo quando interrompida.

#### US-2.1.1: Fala Pausada, Calorosa e Sem Jargões Técnicos
- **Como** usuário que perdeu a visão recentemente,
- **Quero** ouvir uma voz humana, clara e em velocidade confortável (ajustável pelo meu filho), que fale comigo como uma pessoa da família e nunca diga palavras como *"botão não marcado"*, *"ícone"* ou *"toque duas vezes"*,
- **Para que** usar o celular seja agradável e não cansativo.
- **Critérios de Aceite (Gherkin):**
  - **Dado** qualquer texto gerado pelo agente para ser falado ao usuário,
  - **Quando** o `HumanVoiceTtsEngine` sintetizar o áudio,
  - **Então** deve higienizar termos proibidos de UI via filtro léxico (`SanitizerRule`: remove URLs cruas, emojis repetidos, códigos hexadecimais e jargões de sistema),
  - **E** deve aplicar prosódia humanizada (velocidade padrão `0.92x`, tom `1.0x`, pausa de `250ms` após pontos finais antes de fazer uma pergunta ao usuário).

#### US-2.1.2: Repetição Sob Demanda da Última Fala ("Repete" / "O que você disse?")
- **Como** usuário cego em ambiente com barulho na rua ou na TV da sala,
- **Quero** pedir *"Repete"* ou *"Fala mais devagar"* a qualquer momento,
- **Para que** o assistente repita a última informação importante sem que eu precise refazer todo o comando.
- **Critérios de Aceite (Gherkin):**
  - **Dado** que o agente mantém em memória a `lastSpokenImportantMessage`,
  - **Quando** o usuário disser *"Repete"*, *"Não entendi"* ou *"Como é?"*,
  - **Então** o agente repete a última resposta com velocidade reduzida em `-10%` (`0.82x`) para facilitar a compreensão.

---

### FEAT-2.2: Reconhecimento de Voz Offline (STT) com VAD Adaptativo e Anti-Eco
Capta a fala do usuário mesmo sem internet, respeitando pausas naturais de quem está pensando no que vai dizer e impedindo que o microfone grave a própria voz do assistente.

#### US-2.2.1: Detecção Inteligente de Fim de Fala (VAD Tolerante a Pausas)
- **Como** usuário falando uma mensagem ou comando para o celular,
- **Quero** que o microfone não me corte no meio da frase se eu fizer uma pequena pausa para respirar, mas feche rapidamente assim que eu terminar de falar,
- **Para que** eu possa falar no meu ritmo natural sem pressa.
- **Critérios de Aceite (Gherkin):**
  - **Dado** que o microfone entrou no estado `LISTENING`,
  - **Quando** o usuário ainda não começou a falar,
  - **Então** o sistema aguarda até `4.500ms` pelo início da voz antes de dar timeout suave,
  - **E quando** o usuário começa a falar, o detector de atividade de voz (VAD) aguarda `900ms` de silêncio contínuo para comandos curtos (ou `1.500ms` quando estiver ditando uma mensagem para o WhatsApp) antes de encerrar a captura.

#### US-2.2.2: Bloqueio Estrito de Eco Acústico (*Anti-Self-Triggering*)
- **Como** usuário usando o celular no viva-voz sem fone de ouvido,
- **Quero** que o microfone nunca capte a própria voz do assistente falando,
- **Para que** o celular não fique conversando sozinho ou entendendo comandos falsos.
- **Critérios de Aceite (Gherkin):**
  - **Dado** que o agente está no estado `SPEAKING` (TTS ativo),
  - **Quando** a fala terminar e transicionar para `AWAITING_CONFIRMATION`,
  - **Então** o `OfflineSttRecognizer` deve aguardar obrigatoriamente o callback `onDone` do TTS + um *cooldown* acústico de `150ms`, descartar qualquer buffer de áudio residual anterior, emitir o bip curto de escuta e só então abrir a captura do microfone.

---

### FEAT-2.3: Controle de `AudioFocus`, *Audio Ducking* e Roteamento Inteligente
Garante que o som do YouTube ou de um áudio do WhatsApp nunca brigue com a voz do assistente nem atrapalhe o microfone.

#### US-2.3.1: Silenciamento Automático da Mídia durante Escuta e Fala do Agente
- **Como** usuário ouvindo um vídeo no YouTube ou áudio no WhatsApp,
- **Quero** que, no instante em que eu segurar o botão de volume para falar com o agente, o som do vídeo pause ou abaixe automaticamente,
- **Para que** o microfone me escute com perfeição e eu ouça a resposta do agente com clareza.
- **Critérios de Aceite (Gherkin):**
  - **Dado** que há mídia tocando no aparelho (`AudioManager.isMusicActive == true`),
  - **Quando** o gatilho físico acionar o estado `LISTENING`,
  - **Então** o `AudioFocusCoordinator` requisita `AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE` (pausando temporariamente o player ativo),
  - **E quando** o ciclo do comando terminar (sem que o comando tenha sido *"Pausa"*), o foco de áudio é liberado (`abandonAudioFocusRequest`), retomando a mídia onde parou.

---

### FEAT-2.4: Roteador de Intenção em 3 Camadas (*Fast-Path* + *On-Device SLM* + *Cloud*)
Classifica o que o usuário quer fazer pelo caminho mais rápido e seguro possível.

#### US-2.4.1: Resposta Instantânea para Comandos de Controle (*Fast-Path < 50ms*)
- **Como** usuário dando comandos rápidos de controle,
- **Quero** que ordens diretas como *"Sim"*, *"Não"*, *"Para"*, *"Pausa"*, *"Continua"*, *"Toca"*, *"Atende"*, *"Que horas são?"* e *"Quanta bateria tem?"* sejam executadas na mesma hora,
- **Para que** o celular responda rápido como um rádio físico.
- **Catálogo de Intenções Nativas do Roteador:**

| Categoria | Intenção (`IntentType`) | Exemplos de Frases do Usuário | Camada de Resolução |
| :--- | :--- | :--- | :--- |
| **Controle Imediato** | `STOP_OR_CANCEL`, `CONFIRM_YES`, `CONFIRM_NO`, `REPEAT_LAST`, `MEDIA_PAUSE_RESUME`, `MEDIA_NEXT` | *"Para"*, *"Cancela"*, *"Sim"*, *"Pode mandar"*, *"Não"*, *"Repete"*, *"Pausa"*, *"Continua"*, *"Próximo"* | **Camada 1: Fast-Path (< 50ms)** |
| **Utilidades Locais** | `QUERY_TIME_DATE`, `QUERY_BATTERY`, `QUERY_CONNECTIVITY` | *"Que horas são?"*, *"Que dia é hoje?"*, *"Como está a bateria?"*, *"Tem internet?"* | **Camada 1: Fast-Path (< 50ms)** |
| **WhatsApp** | `WA_CHECK_UNREAD`, `WA_READ_CONTACT`, `WA_PLAY_AUDIO`, `WA_SEND_MESSAGE` | *"Tem mensagem nova?"*, *"O que o Gabriel mandou?"*, *"Toca o áudio dele"*, *"Manda um áudio pro Gabriel"* | **Camada 1/2: Regras + SLM Local** |
| **Telefone** | `CALL_MAKE`, `CALL_ANSWER`, `CALL_HANGUP`, `CALL_TOGGLE_SPEAKER` | *"Liga pro meu filho"*, *"Liga pro Gabriel no viva-voz"*, *"Atende"*, *"Desliga"* | **Camada 1/2: Grafo Familiar + Local** |
| **YouTube** | `YT_SEARCH_AND_PLAY`, `YT_PLAY_FAVORITE` | *"Coloca música sertaneja no YouTube"*, *"Põe o jornal da manhã"*, *"Toca minhas músicas"* | **Camada 2: Extrator Local** |
| **Aberto / Visão** | `DESCRIBE_SCREEN_OR_IMAGE`, `GENERAL_QUESTION` | *"O que tem nessa foto que me mandaram?"*, *"O que está escrito nessa tela?"* | **Camada 3: Cloud Gemini (se online) / Resumo Local (se offline)** |

---

## 3. Regras de Negócio e Casos de Borda do Épico 2

- **RN-2.01 (Limite de 2 Opções Cognitivas):** O gerador de diálogo está proibido de oferecer listas longas (*"Você tem mensagens de A, B, C, D e E"*). Ele deve agrupar os 2 contatos mais prioritários (Família > Recentes): *"Você tem mensagens do Gabriel e da Maria, além de outras 3 conversas. Quer ouvir o Gabriel primeiro?"*.
- **RN-2.02 (Fallback Offline Transparente):** Se o usuário fizer uma pergunta que exigiria nuvem (ex: descrever uma foto sem texto) e o aparelho estiver sem internet, o agente nunca exibe erro técnico; ele diz calmamente: *"Estou sem internet agora para ver a imagem, mas consigo ler as mensagens de texto e fazer ligações normalmente"*.
