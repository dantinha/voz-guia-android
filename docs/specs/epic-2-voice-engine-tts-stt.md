# Épico 2: Motor de Voz Humanizada & Entrada Auditiva — Especificação Funcional

- **Projeto:** Voz Guia — Agente Assistivo Android
- **Épico:** 2 de 4
- **Status:** Especificação Pronta para Implementação
- **Componentes Principais:** `VoiceEngineManager`, `PiperTtsEngine`, `OfflineSttEngine`

---

## 1. Objetivo do Épico

Substituir a voz robótica, acelerada e fria do TalkBack por uma experiência de fala calorosa, pausada e humanizada em português brasileiro (pt-BR), associada a um reconhecimento de fala (STT) de baixa latência e alta tolerância a pausas naturais e hesitações normais da fala humana.

---

## 2. Requisitos Funcionais (RF)

- **RF-2.1 (Síntese de Voz Humanizada em pt-BR):**
  - Implementar o `VoiceOutputController` utilizando uma voz neural de alta fidelidade:
    - **Opção A (Offline Máxima):** Motor **Piper TTS** embutido via biblioteca C++/JNI com modelo pt-BR (ex: `pt_BR-faber-medium` ou voz feminina/masculina calorosa, modelo < 60MB).
    - **Opção B (Nativa Rápida):** `android.speech.tts.TextToSpeech` configurado com a melhor voz neural do pacote pt-BR do Android, com pitch calibrado para tom acolhedor (0.95x a 1.0x) e velocidade confortável (0.9x para clareza máxima).
  - O sistema deve suportar interrupção imediata da fala (*barge-in*): se o usuário acionar o botão ou começar a falar, a fala atual do assistente é cortada instantaneamente.
- **RF-2.2 (Reconhecimento de Voz Offline com Baixa Latência):**
  - Motor de STT local baseado em **Vosk** (modelo `vosk-model-small-pt-0.3`) ou **Sherpa-ONNX** embutido no APK:
    - Inicialização rápida na memória.
    - Reconhecimento contínuo de áudio capturado do microfone do aparelho.
    - Suporte a detecção de silêncio com limiar dinâmico (*Voice Activity Detection - VAD*): ao detectar 800ms de silêncio após uma fala, encerra o streaming de áudio e envia o texto transcrito para o orquestrador.
- **RF-2.3 (Diálogo Estruturado em Linguagem Direta):**
  - O agente não usa jargões de software (nada de *"clique aqui"*, *"navegue até"*, *"elemento de interface"*).
  - Toda resposta segue o formato de conversação direta:
    - Afirmativa curta: *"Entendido."* / *"Pronto."*
    - Informação sintetizada: *"O Gabriel mandou uma mensagem."*
    - Pergunta de confirmação: *"Quer que eu leia ou responda?"*
- **RF-2.4 (Controle de Fone de Ouvido & Viva-Voz Inteligente):**
  - Se o usuário estiver usando fone de ouvido (com fio ou Bluetooth), todo o áudio do assistente sai exclusivamente no fone com privacidade.
  - Se estiver sem fone de ouvido, o assistente pode operar no alto-falante principal (ou no auricular ao aproximar o telefone do ouvido via sensor de proximidade `Sensor.TYPE_PROXIMITY`).

---

## 3. Critérios de Aceite (Definition of Done)

- [ ] **CA-2.1:** O motor de TTS reproduz frases em português com entonação natural, sem cortes de final de frase e sem travar a interface.
- [ ] **CA-2.2:** O STT local transcreve comandos típicos ("Liga pro Gabriel", "Tem mensagem nova?", "Abre o YouTube") em modo 100% avião (sem internet) com taxa de acerto superior a 90%.
- [ ] **CA-2.3:** Ao acionar o botão durante a fala do agente, a reprodução é interrompida em menos de 100ms.
