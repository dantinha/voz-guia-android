# Épico 3: Módulos de Ação Autônoma (WhatsApp, Telefone & YouTube) — Especificação Funcional Detalhada

- **Projeto:** Voz Guia — Agente Assistivo Android
- **Épico:** 3 de 5
- **Versão:** 2.0 (Senior PM Review)
- **Prioridade:** P0 (Core de Valor Diário do Usuário)
- **Componentes de Arquitetura:** `WhatsAppCopilotModule`, `NotificationListenerBridge`, `PhoneAndVoipCallModule`, `YouTubeRadioModule`, `YouTubeAdSkipperDaemon`

---

## 1. Visão Geral e Valor de Produto do Épico

O pai do Gabriel usa essencialmente **três aplicativos**: **WhatsApp**, **Telefone** e **YouTube**. Cada um deles possui armadilhas severas para quem não enxerga:
- No **WhatsApp**, grupos barulhentos escondem as mensagens dos filhos, botões de Play de áudio são minúsculos e gravar áudio exige segurar um ícone no canto inferior direito (que frequentemente escapa do dedo e cancela a gravação).
- No **Telefone**, ligações chegam tanto pela rede celular comum (GSM) quanto por **chamadas de voz do WhatsApp**, cada uma com uma tela diferente.
- No **YouTube**, propagandas de 2 a 5 minutos interrompem a música esperando um clique no botão *"Pular anúncio"*, e buscas comuns frequentemente caem em *YouTube Shorts* (vídeos curtos de 15 segundos que repetem sem parar).

O **Épico 3** automatiza completamente esses três aplicativos com inteligência defensiva.

---

## 2. Decomposição em Features e User Stories

### FEAT-3.1: WhatsApp Copiloto (`NotificationListener` + `AccessibilityService`)
Opera em arquitetura dupla: usa o `NotificationListenerService` para ler e responder mensagens instantaneamente em background (sem precisar navegar telas) e usa o `AccessibilityService` quando precisa abrir a conversa para dar Play em áudios recebidos ou enviar notas de voz.

#### US-3.1.1: Resumo Inteligente de Mensagens Não Lidas com Prioridade Familiar
- **Como** usuário cego querendo saber quem falou comigo no WhatsApp,
- **Quero** perguntar *"Tem mensagem nova?"* e ouvir primeiro as mensagens da minha família e contatos favoritos, ignorando grupos barulhentos,
- **Para que** eu vá direto ao que importa sem ouvir dezenas de notificações irrelevantes.
- **Critérios de Aceite (Gherkin):**
  - **Dado** que o `NotificationListenerBridge` interceptou notificações do pacote `com.whatsapp`,
  - **Quando** o usuário perguntar *"Tem mensagem nova?"* (ou *"O que mandaram no WhatsApp?"*),
  - **Então** o sistema filtra mensagens de grupos (a menos que o usuário peça explicitamente *"E nos grupos?"*), ordena os remetentes priorizando contatos marcados como `Família/VIP` no Épico 4,
  - **E** sintetiza em uma única frase clara: *"Seu filho Gabriel mandou 1 mensagem de voz há 5 minutos, e a Maria mandou: 'Bom dia, já tomou café?'. Quer ouvir o áudio do Gabriel agora?"*.

#### US-3.1.2: Reprodução Automática em Fila de Mensagens de Voz (Áudios)
- **Como** usuário cego que recebeu um ou mais áudios no WhatsApp,
- **Quero** apenas dizer *"Sim"* ou *"Toca o áudio do Gabriel"* para que o celular abra a conversa e dê play sozinho no áudio não ouvido,
- **Para que** eu nunca precise caçar o botãozinho de Play na tela.
- **Critérios de Aceite (Gherkin):**
  - **Dado** que há uma mensagem de voz de um contato no WhatsApp,
  - **Quando** o usuário comandar *"Toca"* / *"Ouve o Gabriel"*,
  - **Então** o módulo abre a conversa do contato (via `PendingIntent` da notificação ou Intent direta do número),
  - **E** localiza na árvore `AccessibilityNodeInfo` os nós de áudio (identificados por descrições contendo `"Mensagem de voz"`, `"Reproduzir"` ou barra de duração `mm:ss`), clica no primeiro áudio não reproduzido (`ACTION_CLICK`),
  - **E** caso haja 2 áudios seguidos do mesmo remetente, o WhatsApp reproduz em sequência e, ao terminar, o agente pergunta suavemente: *"Fim do áudio do Gabriel. Quer responder?"*.

#### US-3.1.3: Envio de Mensagem (Texto Rápido ou Áudio Guiado) Sem Segurar a Tela
- **Como** usuário cego querendo responder ou mandar mensagem para alguém no WhatsApp,
- **Quero** falar a mensagem normalmente depois do bip e confirmar o envio por voz, sem precisar ficar segurando o ícone pequenininho de microfone do WhatsApp com o dedo,
- **Para que** eu não corra o risco de escorregar o dedo e perder tudo o que falei.
- **Critérios de Aceite (Gherkin):**
  - **Dado** que o usuário disse *"Responde pro Gabriel"* ou *"Manda mensagem pro meu filho"*,
  - **Quando** o agente emitir o sinal `"Pode falar a mensagem"`,
  - **Então** o usuário dita a mensagem livremente, o agente repete o que entendeu (*"Você disse: 'Já almocei, obrigado, filho'. Posso enviar?"*),
  - **E quando** o usuário confirmar com *"Sim"* ou *"Manda"*,
  - **Então** o agente envia instantaneamente via `RemoteInput` da notificação ativa (em menos de `200ms`, sem nem precisar abrir a tela) ou, se não houver notificação ativa, abre o chat do contato, preenche o campo de texto (`ACTION_SET_TEXT`) e clica no botão `Enviar`, emitindo a vibração dupla de sucesso: *"Mensagem enviada para Gabriel"*.

---

### FEAT-3.2: Telefone & Chamadas Unificadas (Operadora GSM + WhatsApp Voz)
Centraliza o ato de ligar e atender chamadas, seja pela linha telefônica comum ou por ligação de voz do WhatsApp, usando apenas a voz e o botão físico de volume.

#### US-3.2.1: Discagem por Apelido Familiar com Viva-Voz Automático
- **Como** usuário cego querendo falar com alguém por telefone,
- **Quero** dizer *"Liga pro meu filho"* ou *"Liga pro Gabriel"* e ter a chamada iniciada já no viva-voz (quando eu não estiver de fone),
- **Para que** eu possa conversar confortavelmente segurando o celular ou deixando-o apoiado na mesa.
- **Critérios de Aceite (Gherkin):**
  - **Dado** que o usuário pediu *"Liga pro [Nome ou Apelido]"*,
  - **Quando** o contato for único e de confiança alta (`isTrustedFamily == true`),
  - **Então** o agente anuncia *"Ligando para Gabriel no viva-voz"*, dispara `Intent(Intent.ACTION_CALL, Uri.parse("tel:$number"))` e ativa `AudioManager.isSpeakerphoneOn = true` (se nenhum fone Bluetooth/cabeado estiver conectado),
  - **Mas quando** houver mais de um contato com o mesmo primeiro nome (ex: *"João Silva"* e *"João Pedro"*),
  - **Então** o agente pergunta antes de discar: *"Encontrei João Silva e João Pedro. Para qual dos dois você quer ligar?"*.

#### US-3.2.2: Anúncio Falado de Quem Está Ligando e Atendimento com 1 Clique no Volume
- **Como** usuário cego quando o telefone começa a tocar,
- **Quero** que o celular fale em voz clara quem está me ligando (tanto em ligação normal quanto em ligação de WhatsApp) e me permita atender apertando 1 vez o botão de volume,
- **Para que** eu nunca perca uma ligação por não conseguir arrastar o botão na tela.
- **Critérios de Aceite (Gherkin):**
  - **Dado** que o aparelho entrou no estado `CALL_STATE_RINGING` (GSM) ou recebeu notificação de chamada de voz do WhatsApp (`category == CATEGORY_CALL`),
  - **Quando** o toque iniciar,
  - **Então** o agente reduz momentaneamente o volume da campainha e anuncia repetidamente a cada 4 segundos: *"Ligação de [Nome do Contato]. Aperte o botão de volume para atender, ou segure para recusar"*,
  - **Quando** o usuário der **1 toque curto no botão de Volume**,
  - **Então** a chamada é atendida imediatamente via `TelecomManager.acceptRingingCall()` (para GSM) ou via clique na ação `"Atender"` da notificação/tela do WhatsApp, ativando o viva-voz conforme preferência do perfil,
  - **E quando** a chamada estiver em andamento e o usuário quiser desligar, segurar o botão de Volume por 1 segundo encerra a chamada (`TelecomManager.endCall()`).

---

### FEAT-3.3: YouTube em Modo Rádio Blindado (Auto-Play, *Ad-Skipper* & *Anti-Shorts*)
Transforma o aplicativo oficial do YouTube em um rádio de voz simples, contínuo e livre de armadilhas visuais.

#### US-3.3.1: Busca por Voz e Play Automático no Primeiro Vídeo Completo (Anti-Shorts)
- **Como** usuário cego querendo ouvir música, noticiário, futebol ou programa no YouTube,
- **Quero** dizer *"Coloca música sertaneja raiz no YouTube"* ou *"Coloca as notícias de hoje"* e ter o programa tocando em poucos segundos sem que eu toque na tela,
- **Para que** o YouTube funcione como um rádio inteligente sob meu comando.
- **Critérios de Aceite (Gherkin):**
  - **Dado** que o usuário pediu para tocar um tema no YouTube,
  - **Quando** o `YouTubeRadioModule` abrir os resultados de busca no pacote `com.google.android.youtube`,
  - **Então** o parser de árvore percorre os itens de resultado, **ignora anúncios patrocinados (`Patrocinado` / `Ad`) e carrosséis de `Shorts`** (vídeos com menos de 1 minuto ou marcados como `Shorts`),
  - **E** clica automaticamente no primeiro vídeo longo/playlist orgânica, anunciando: *"Tocando [Título resumido do vídeo]"*.

#### US-3.3.2: Pulador Automático de Anúncios em Background (*Silent Ad-Skipper*)
- **Como** usuário cego ouvindo um vídeo ou programa no YouTube,
- **Quero** que o agente clique sozinho no botão *"Pular anúncio"* sempre que uma propaganda interromper o meu programa,
- **Para que** eu não fique preso ouvindo comerciais de 5 minutos sem conseguir voltar para a minha música.
- **Critérios de Aceite (Gherkin):**
  - **Dado** que o pacote ativo na tela é `com.google.android.youtube`,
  - **Quando** o `YouTubeAdSkipperDaemon` detectar na árvore de acessibilidade um nó clicável com ID `skip_ad_button` ou texto/descrição `"Pular anúncio"`, `"Pular anúncios"` ou `"Skip ad"`,
  - **Então** ele executa `performAction(ACTION_CLICK)` automaticamente em menos de `200ms` de forma silenciosa (sem interromper o usuário com fala),
  - **E** caso apareça o diálogo *"Continuar assistindo?"* que pausa a reprodução após longo tempo, ele clica automaticamente em *"Sim"* para manter o som tocando.

#### US-3.3.3: Controle de Rádio durante Reprodução (Pausar, Continuar, Próximo)
- **Como** usuário ouvindo o YouTube,
- **Quero** poder segurar o botão de volume e pedir *"Pausa"*, *"Continua"*, *"Próxima música"* ou *"Troca para notícias"*,
- **Para que** eu tenha controle total do que estou ouvindo sem tocar na tela.
- **Critérios de Aceite (Gherkin):**
  - **Dado** que o YouTube está reproduzindo mídia,
  - **Quando** o usuário acionar o gatilho físico e disser *"Pausa"*, *"Continua"* ou *"Próximo"*,
  - **Então** o módulo despacha `KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE` ou `KEYCODE_MEDIA_NEXT` via `AudioManager.dispatchMediaKeyEvent` (que funciona mesmo com a tela do YouTube minimizada) e confirma com um bip curto.

---

## 3. Regras de Negócio e Casos de Borda do Épico 3

- **RN-3.01 (Proteção contra Envio Acidental no WhatsApp):** O agente **JAMAIS** envia uma mensagem no WhatsApp sem antes repetir em voz alta o texto compreendido e o nome do destinatário, exigindo confirmação explícita (*"Sim"*, *"Pode mandar"*, *"Envia"*). Qualquer outra palavra ou silêncio cancela o envio por segurança.
- **RN-3.02 (Fallback de Mudança de Layout do WhatsApp/YouTube):** Nunca depender apenas de um único `viewIdResourceName` fixo (pois o WhatsApp ofusca ou muda IDs em atualizações). O localizador de elementos (`SemanticNodeMatcher`) deve usar estratégia em cascata: (1) `viewIdResourceName`, (2) `contentDescription` em pt-BR/en, (3) classe do componente + posição relativa na tela.
