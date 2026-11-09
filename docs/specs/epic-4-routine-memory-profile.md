# Épico 4: Memória de Rotina, Grafo Familiar & Autonomia Diária — Especificação Funcional Detalhada

- **Projeto:** Voz Guia — Agente Assistivo Android
- **Épico:** 4 de 5
- **Versão:** 2.0 (Senior PM Review)
- **Prioridade:** P0/P1 (Personalização, Contexto Familiar e Autonomia Pessoal)
- **Componentes de Arquitetura:** `FamilyGraphDatabase`, `PhoneticAliasResolver`, `DailyAutonomyUtilities`, `RoutineHabitLearner`, `MorningBriefingOrchestrator`

---

## 1. Visão Geral e Valor de Produto do Épico

Um assistente comum trata seu usuário como um estranho toda vez que o microfone abre: se o usuário disser *"Liga pro meu filho"* ou *"Toca aquela rádio que eu gosto"*, o assistente genérico responde *"Não entendi"* ou pede configurações complexas. Além disso, para quem perdeu a visão recentemente, tarefas simples do cotidiano — saber se já é dia ou noite, quantos por cento de bateria restam no celular ou lembrar a hora do remédio — dependem hoje de perguntar para outra pessoa em casa.

O **Épico 4** dá ao **Voz Guia** uma memória afetiva e funcional 100% local:
1. Conhece a família pelo nome, grau de parentesco e variações fonéticas de fala.
2. Responde instantaneamente sobre hora, data, dia da semana, clima local, nível de bateria e alarmes/remédios.
3. Aprende os programas e músicas que ele realmente gosta de ouvir em cada período do dia.

---

## 2. Decomposição em Features e User Stories

### FEAT-4.1: Grafo Familiar e Dicionário Fonético Local (`SQLite / Room`)
Mapeia relações afetivas (*"meu filho"*, *"minha filha"*, *"minha esposa"*, *"meu irmão"*) e variações de pronúncia do STT para os números de telefone e conversas do WhatsApp corretos.

#### US-4.1.1: Resolução de Parentesco e Apelidos Afetivos
- **Como** usuário cego falando naturalmente,
- **Quero** me referir às pessoas da minha família como eu falo em casa (*"meu filho"*, *"o Gabriel"*, *"o Biel"*, *"a Maria"*),
- **Para que** o celular saiba exatamente de quem estou falando tanto no Telefone quanto no WhatsApp.
- **Critérios de Aceite (Gherkin):**
  - **Dado** que o banco SQLite local possui a tabela `trusted_contacts` com campos `id`, `display_name`, `phone_number`, `whatsapp_jid`, `relationship_role` (ex: `"filho"`), `phonetic_aliases` (ex: `["gabriel", "gabrieu", "biel", "meu filho"]`) e `is_vip_family: Boolean`,
  - **Quando** o STT transcrever *"Manda mensagem pro meu filho"* ou *"Liga pro Gabrieu"*,
  - **Então** o `PhoneticAliasResolver` (usando distância fonética Metaphone pt-BR + Levenshtein $\le 2$) resolve deterministicamente para o registro de `Gabriel` em menos de `10ms`.

#### US-4.1.2: Aprendizado Conversacional de Novo Apelido ("Quem é...?")
- **Como** usuário que mencionou um parente ou amigo ainda não associado a um apelido,
- **Quero** que o assistente me pergunte uma única vez quem é essa pessoa e grave para sempre,
- **Para que** da próxima vez ele já saiba sem perguntar.
- **Critérios de Aceite (Gherkin):**
  - **Dado** que o usuário disse *"Liga pro meu irmão"* e o papel `"irmão"` ainda não está vinculado em `trusted_contacts`,
  - **Quando** o resolvedor não encontrar o vínculo,
  - **Então** o agente pergunta: *"Qual é o nome do seu irmão na agenda?"*,
  - **E quando** o usuário responder *"Carlos"* (e o usuário confirmar a associação com `"Carlos Almeida"` da agenda), o vínculo `"irmão" → Carlos Almeida` é salvo permanentemente no SQLite.

---

### FEAT-4.2: Utilidades Instantâneas de Autonomia Pessoal (Hora, Data, Bateria e Lembretes de Remédio)
Permite que o usuário mantenha sua orientação no tempo e cuide de sua rotina sem depender de terceiros.

#### US-4.2.1: Consulta Rápida de Hora, Dia da Semana e Bateria
- **Como** usuário cego querendo me situar no dia,
- **Quero** perguntar *"Que horas são?"*, *"Que dia é hoje?"* ou *"Como está a bateria?"* e receber uma resposta imediata e completa,
- **Para que** eu tenha total independência sobre o meu tempo e sobre quando colocar o celular na tomada.
- **Critérios de Aceite (Gherkin):**
  - **Dado** que o usuário perguntou *"Que horas são?"* (mesmo sem internet),
  - **Quando** o `Fast-Path` acionar `DailyAutonomyUtilities`,
  - **Então** o agente responde em menos de `150ms` dizendo o período do dia: *"São 3 horas e 20 minutos da tarde, segunda-feira, dia 9 de novembro. A bateria está em 78%"*.

#### US-4.2.2: Alarme Falado de Remédios e Compromissos com Confirmação Vocal
- **Como** usuário que toma medicamentos em horários certos,
- **Quero** que o celular me avise em voz alta na hora do remédio (*"Hora de tomar o remédio da pressão"*) e pare o alarme quando eu apertar o botão de volume,
- **Para que** eu nunca esqueça uma dose nem fique com um alarme tocando sem conseguir desligar na tela.
- **Critérios de Aceite (Gherkin):**
  - **Dado** um lembrete cadastrado na tabela `routine_reminders` (`title`, `time_hh_mm`, `days_of_week`, `is_active`),
  - **Quando** o `AlarmManager` disparar no horário exato (`setExactAndAllowWhileIdle`),
  - **Então** o celular vibra e anuncia em voz clara: *"Atenção: são [HH] horas. Hora de [Título do Lembrete]. Aperte o botão de volume para confirmar que ouviu"*,
  - **E quando** o usuário der 1 clique no botão de Volume, o alarme é encerrado e registrado como `confirmed_at` no histórico do dia.

---

### FEAT-4.3: Aprendizado de Favoritos de Mídia e Rádio Pessoal
Aprende o gosto cultural/musical do usuário para que ele não precise especificar nomes complicados de canais do YouTube.

#### US-4.3.1: Atalho Vocal "Coloca Minhas Músicas" / "Coloca Meu Jornal"
- **Como** usuário que gosta de ouvir sempre os mesmos estilos musicais ou programas de notícias/esporte,
- **Quero** apenas dizer *"Coloca minhas músicas"* ou *"Coloca as notícias"* e ouvir o que eu mais gosto no horário,
- **Para que** seja tão simples quanto ligar um rádio antigo na estação favorita.
- **Critérios de Aceite (Gherkin):**
  - **Dado** que a tabela `media_favorites` registra os termos e canais que o usuário ouviu por mais de 5 minutos (separados por faixa de horário: `MANHA`, `TARDE`, `NOITE`),
  - **Quando** o usuário disser um comando genérico como *"Toca uma música"* ou *"Coloca meu programa"*,
  - **Então** o `RoutineHabitLearner` seleciona o favorito nº 1 daquela faixa de horário (ou o favorito configurado pelo filho no Modo Cuidador) e inicia a reprodução no YouTube dizendo: *"Colocando seu favorito: [Nome do Programa/Estilo]"*.

---

### FEAT-4.4: Briefing Matinal Acolhedor
Transforma o primeiro contato do dia com o celular em um momento acolhedor e informativo.

#### US-4.4.1: Saudação Personalizada no Primeiro Acionamento da Manhã
- **Como** usuário cego acordando pela manhã,
- **Quero** que, na primeira vez que eu pegar o celular depois das 06:00 da manhã, o assistente me dê bom dia, diga que dia é hoje, avise os lembretes do dia e resuma se meus filhos mandaram mensagem,
- **Para que** eu comece o dia informado e acompanhado.
- **Critérios de Aceite (Gherkin):**
  - **Dado** que é o primeiro acionamento do agente após as `06:00` da manhã (`last_morning_briefing_date != today`),
  - **Quando** o usuário acionar o botão físico pela primeira vez no dia,
  - **Então** o agente saúda: *"Bom dia, seu [Nome]! Hoje é [Dia da Semana], [Dia] de [Mês]. A bateria está em [XX]%. [Resumo de mensagens familiares]. O que o senhor gostaria de fazer agora?"*.
