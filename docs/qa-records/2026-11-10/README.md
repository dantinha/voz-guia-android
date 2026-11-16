# Registro Permanente de QA e Testes — Voz Guia Android (2026-11-10)

- **Data:** 10/11/2026
- **Build Validado:** `app/build/outputs/apk/debug/app-debug.apk` (`26,77 MB`)
- **Emulador:** `medium_phone` (`emulator-5554`, `AEHD 2.2`, Android `sdk_gphone64_x86_64`)

---

## 🎥 Gravação de Tela e Evidências Visuais

- **Vídeo Completo (MP4, 31s, 1,85 MB):** [`vozguia_qa_emulator_demo.mp4`](vozguia_qa_emulator_demo.mp4)
  - `00:00`–`00:04` — Aba **Blindagem** com todas as **6 permissões críticas ativas (`✅ ATIVO`)**: Serviço de Acessibilidade, Acesso a Notificações, Chamadas Telefônicas, Microfone, Sobrepor a Outros Apps e Bateria Irrestrita Motorola.
  - `00:05`–`00:23` — Aba **Família**: exibição dos contatos persistidos (*Gabriel Silva* SOS, *Maria Silva*) e cadastro em tempo real de *Carlos Almeida* (`irmao`, `meu irmao`) no banco **Room SQLite**.
  - `00:24`–`00:29` — Aba **Rotina**: acionamento do teste de voz (*Ouvir Teste*), visualização do favorito *Moda de Viola (TARDE)* persistido e formulário *Novo Lembrete de Remédio*.
  - `00:30`–`00:31` — Aba **Diagnóstico**: tela limpa de observabilidade local sem comandos pendentes.
- **Capturas de Tela Verificadas:**
  - [`qa_01_blindagem_ativo.png`](qa_01_blindagem_ativo.png)
  - [`qa_02_familia_room_sqlite.png`](qa_02_familia_room_sqlite.png)
  - [`qa_03_rotina_tts_favoritos.png`](qa_03_rotina_tts_favoritos.png)
  - [`qa_04_diagnostico.png`](qa_04_diagnostico.png)

---

## 🧪 Resumo dos Testes Unitários (`.\gradlew.bat testDebugUnitTest`)

- **Resultado Geral:** **34 suítes / 150 testes executados — 0 falhas, 0 erros, 0 pulados**
- **Log Completo por Suíte:** [`junit_34_suites_results.txt`](junit_34_suites_results.txt)
- **Logs do Dispositivo (`adb logcat -s AndroidRuntime:E`):** Zero exceções durante todo o fluxo.
