# Épico 3: Módulos Especializados de Ação Autônoma — Especificação Funcional

- **Projeto:** Voz Guia — Agente Assistivo Android
- **Épico:** 3 de 4
- **Status:** Especificação Pronta para Implementação
- **Componentes Principais:** `WhatsAppAutomationModule`, `PhoneCallModule`, `YouTubeAudioModule`

---

## 1. Objetivo do Épico

Resolver de forma direta e sem esforço os três fluxos que o pai do Gabriel mais utiliza e onde o TalkBack mais falha:
1. **WhatsApp:** ouvir áudios e mensagens de texto sem precisar caçar a conversa nem acertar o botão de play minúsculo, e enviar áudios guiados por voz.
2. **Telefone:** fazer chamadas diretas por voz para contatos familiares com opção de viva-voz automático.
3. **YouTube (Modo Rádio):** pesquisar músicas, programas e podcasts por voz, dando play automático imediatamente.

---

## 2. Requisitos Funcionais (RF)

### 2.1 Módulo WhatsApp (`WhatsAppAutomationModule`)
- **RF-3.1.1 (Leitura e Síntese de Mensagens Não Lidas):**
  - Quando o usuário fala *"Tem mensagem nova?"* ou aciona o agente com o WhatsApp aberto:
    - O módulo localiza as notificações ativas ou varre a lista de conversas via `AccessibilityService`.
    - Agrupa por remetente e sintetiza: *"Você tem 2 mensagens novas: um áudio do Gabriel recebido há 10 minutos e um texto da Maria. Quer ouvir o Gabriel primeiro?"*.
- **RF-3.1.2 (Reprodução Automática de Áudios do WhatsApp):**
  - Ao comando *"Toca o áudio"* ou *"Ouve o Gabriel"*:
    - O `AccessibilityService` navega até a conversa do contato solicitado.
    - Localiza o último nó de mensagem de voz (`contentDescription` contendo "Mensagem de voz" ou botão de play com identificador de reprodução).
    - Executa `node.performAction(AccessibilityNodeInfo.ACTION_CLICK)`.
    - Informa ao usuário com uma vibração sutil: *"Tocando áudio de [Nome]..."*.
- **RF-3.1.3 (Envio de Áudio Guiado por Voz):**
  - Ao comando *"Manda um áudio pro Gabriel"*:
    - O assistente abre a conversa do Gabriel no WhatsApp.
    - Avisa: *"Pode falar a mensagem após o sinal"*. Emite o bip imediato.
    - Grava o áudio do usuário ou digita por voz no campo de texto.
    - Confirma com o usuário: *"Gravei sua mensagem: '[prévia falada]'. Posso enviar?"*.
    - Ao ouvir *"Sim"* ou *"Envia"*, localiza o botão de envio no WhatsApp e clica automaticamente.

### 2.2 Módulo Telefone & Chamadas (`PhoneCallModule`)
- **RF-3.2.1 (Discagem Inteligente por Voz e Apelidos Familiares):**
  - Ao comando *"Liga pro Gabriel"* ou *"Liga pro meu filho"*:
    - Consulta a agenda de contatos ou a lista de apelidos da memória de rotina.
    - Confirma o contato: *"Ligando para Gabriel..."*.
    - Dispara a Intent nativa de ligação (`Intent.ACTION_CALL`) diretamente com o número.
- **RF-3.2.2 (Viva-Voz Automático Opcional):**
  - Se configurado no perfil ou solicitado por voz (*"Liga no viva-voz"*), ativa o alto-falante externo via `AudioManager.setSpeakerphoneOn(true)` assim que a chamada for estabelecida.
- **RF-3.2.3 (Atendimento de Chamadas por Botão Físico):**
  - Quando o telefone tocar (chamada recebida):
    - O assistente fala quem está ligando: *"Gabriel está ligando. Aperte o botão de volume para atender ou segure para recusar"*.
    - Um toque curto no botão de volume atende a chamada imediatamente (dispensa o gesto de arrastar a tela para o lado).

### 2.3 Módulo YouTube no Modo Rádio (`YouTubeAudioModule`)
- **RF-3.3.1 (Busca e Reprodução Automática):**
  - Ao comando *"Toca sertanejo antigo no YouTube"* ou *"Coloca o podcast de notícias"*:
    - Abre o aplicativo do YouTube via Intent explícita com a query de busca (`vnd.youtube:search?...` ou `Intent.ACTION_SEARCH`).
    - O `AccessibilityService` detecta a abertura dos resultados e clica automaticamente no primeiro vídeo orgânico relevante.
    - Maximiza o player e começa a tocar imediatamente sem que o usuário precise procurar na tela.
- **RF-3.3.2 (Controles Simples por Voz ou Botão):**
  - Durante a reprodução no YouTube:
    - Segurar o botão de volume e falar: *"Pausa"*, *"Continua"*, *"Próximo vídeo"*.
    - O agente executa a ação e confirma com feedback sonoro imediato.

---

## 3. Critérios de Aceite (Definition of Done)

- [ ] **CA-3.1:** Falar "Manda um áudio pro Gabriel" abre a conversa certa no WhatsApp e conclui o envio com confirmação por voz.
- [ ] **CA-3.2:** Falar "Tem mensagem nova?" resume corretamente a lista de conversas sem ler textos de publicidade ou cabeçalhos técnicos.
- [ ] **CA-3.3:** Falar "Liga pro Gabriel" inicia a chamada telefônica sem que o usuário precise tocar na tela uma única vez.
- [ ] **CA-3.4:** Falar "Toca rádio no YouTube" abre o YouTube e inicia a reprodução do primeiro resultado em menos de 3 segundos.
