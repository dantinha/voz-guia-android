# Épico 5: Modo Cuidador (Familiar), Segurança Operacional & Resiliência — Especificação Funcional Detalhada

- **Projeto:** Voz Guia — Agente Assistivo Android
- **Épico:** 5 de 5
- **Versão:** 2.0 (Senior PM Review)
- **Prioridade:** P0/P1 (Configuração pelo Filho, Segurança de Vida e Estabilidade Android 14/15)
- **Componentes de Arquitetura:** `CaregiverDashboardActivity`, `MotorolaPermissionWizard`, `BatteryAndWatchdogMonitor`, `EmergencySosDispatcher`

---

## 1. Visão Geral e Valor de Produto do Épico

Um aplicativo para uma pessoa cega nunca é usado apenas por ela no momento da instalação: **quem instala, concede as permissões críticas do Android, cadastra os contatos iniciais e presta suporte quando algo muda é o familiar vidente (o Gabriel)**.

Além disso, smartphones Android modernos (especialmente com gerenciamento de bateria da Motorola) matam serviços em segundo plano se não forem devidamente blindados, e uma pessoa cega não consegue ler avisos de *"Bateria fraca: 5%"* nem destravar o celular sozinha se passar mal.

O **Épico 5** entrega:
1. **O Painel do Familiar (*Modo Cuidador*):** Interface visual limpa e rápida para o Gabriel configurar tudo para o pai em 3 minutos.
2. **O Assistente de Blindagem Motorola Edge 50 Neo:** Guia de 1 clique para conceder todas as permissões e travar o serviço contra fechamento em background.
3. **Guardião Proativo de Bateria e Comando SOS de Emergência:** Avisos falados de bateria baixa/carregador conectado e discagem de emergência imediata para o filho.

---

## 2. Decomposição em Features e User Stories

### FEAT-5.1: Painel Visual do Familiar (*Modo Cuidador*) & Assistente de Permissões
Tela visual em Jetpack Compose (*Anti-AI Design*: limpa, legível e direta) exclusiva para o Gabriel configurar o aparelho do pai.

#### US-5.1.1: Wizard de Permissões e Blindagem no Motorola Edge 50 Neo
- **Como** familiar (Gabriel) configurando o celular pela primeira vez,
- **Quero** ver um checklist claro com botões diretos que me levem a cada permissão necessária no Android 14/15 (`AccessibilityService`, `NotificationListener`, `Ligar/Atender Chamadas`, `Microfone`, `Sobreposição de Tela` e `Uso Irrestrito de Bateria`),
- **Para que** eu deixe o aparelho 100% configurado e garantido contra travamentos sem precisar procurar menus escondidos nas configurações da Motorola.
- **Critérios de Aceite (Gherkin):**
  - **Dado** que o Gabriel abriu o aplicativo `Voz Guia`,
  - **Quando** qualquer uma das 6 permissões críticas estiver pendente,
  - **Então** o painel exibe cards com status visual (`✅ Ativo` vs `⚠️ Pendente — Clique para Ativar`), abrindo diretamente a Intent de configuração do Android correspondente (incluindo `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`).

#### US-5.1.2: Gestão Visual de Contatos VIP, Apelidos, Favoritos do YouTube e Voz
- **Como** familiar cuidando da experiência do meu pai,
- **Quero** cadastrar na tela como meu pai me chama (*"meu filho"*, *"Gabriel"*), adicionar os horários dos remédios dele, cadastrar os canais/estilos do YouTube que ele gosta e testar a velocidade e o tom da voz na hora,
- **Para que** o celular já comece sabendo toda a rotina dele desde o primeiro dia de uso.
- **Critérios de Aceite (Gherkin):**
  - **Dado** que o Gabriel está na aba **"Rotina & Família"** do Modo Cuidador,
  - **Quando** ele adicionar um Contato VIP (selecionando da agenda + definindo o parentesco e apelidos) ou ajustar o slider de velocidade da voz (`0.75x` a `1.20x`) clicando em `"Ouvir Teste"`,
  - **Então** as configurações são salvas imediatamente no SQLite e aplicadas ao serviço em tempo real.

#### US-5.1.3: Histórico de Comandos Não Compreendidos (*Observability Log Local*)
- **Como** familiar querendo melhorar o assistente para o meu pai,
- **Quero** ver uma lista simples das últimas frases que o meu pai falou e que o agente teve dificuldade de entender, podendo criar um atalho/apelido para elas com 1 clique,
- **Para que** o assistente fique cada vez mais adaptado ao jeito exato de o meu pai falar.
- **Critérios de Aceite (Gherkin):**
  - **Dado** que uma fala do usuário caiu em `UNKNOWN_INTENT` ou foi cancelada,
  - **Quando** o Gabriel abrir a aba **"Diagnóstico & Aprendizado"**,
  - **Então** ele vê o texto transcrito pelo STT, o horário e um botão **"Vincular a uma Ação/Contato"**.

---

### FEAT-5.2: Guardião Proativo de Bateria, Carregador & Comando de Emergência SOS
Garante segurança física e autonomia energética para que o pai nunca fique incomunicável com o celular descarregado.

#### US-5.2.1: Avisos Falados de Bateria Baixa e Confirmação de Carregador na Tomada
- **Como** usuário cego que não enxerga o ícone de bateria no canto da tela,
- **Quero** que o celular me avise em voz alta quando a bateria chegar em 20% e 10%, e também fale *"Carregando bateria"* no exato segundo em que eu espetar o cabo na tomada,
- **Para que** eu tenha certeza absoluta de que o cabo está funcionando e que meu celular não vai desligar sozinho.
- **Critérios de Aceite (Gherkin):**
  - **Dado** que o `BatteryAndWatchdogMonitor` escuta `Intent.ACTION_POWER_CONNECTED`, `ACTION_POWER_DISCONNECTED` e `ACTION_BATTERY_CHANGED`,
  - **Quando** o cabo do carregador for conectado,
  - **Então** o celular vibra e fala imediatamente: *"Carregador conectado. Bateria em [XX] por cento"*,
  - **E quando** a bateria atingir `20%` e `10%` fora do carregador, o agente anuncia em voz clara: *"Atenção: a bateria do celular está fraca, com [XX] por cento. Por favor, coloque para carregar"*.

#### US-5.2.2: Comando de Emergência Familiar ("Preciso de Ajuda" / "Emergência")
- **Como** usuário cego em uma situação de mal-estar, queda ou urgência em casa,
- **Quero** poder falar *"Socorro"*, *"Emergência"* ou *"Preciso de ajuda"* (ou segurar os dois botões de volume juntos por 3 segundos),
- **Para que** o celular ligue imediatamente no viva-voz para o meu filho Gabriel e envie uma mensagem no WhatsApp avisando que pedi ajuda.
- **Critérios de Aceite (Gherkin):**
  - **Dado** que o comando `EMERGENCY_SOS` foi acionado por voz ou combinação física de botões,
  - **Quando** o `EmergencySosDispatcher` iniciar,
  - **Então** ele anuncia *"Ligando agora para emergência: Gabriel"*, coloca o volume do viva-voz em `100%` e inicia a chamada telefônica imediata para o contato de emergência nº 1 cadastrado no Modo Cuidador.
