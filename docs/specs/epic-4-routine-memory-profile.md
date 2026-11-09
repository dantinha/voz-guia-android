# Épico 4: Memória de Rotina & Perfil Pessoal — Especificação Funcional

- **Projeto:** Voz Guia — Agente Assistivo Android
- **Épico:** 4 de 4
- **Status:** Especificação Pronta para Implementação
- **Componentes Principais:** `RoutineMemoryStore`, `PersonalProfileManager`

---

## 1. Objetivo do Épico

Fazer com que o agente não seja apenas um executor de comandos frio, mas um **copiloto que aprende a rotina do pai do Gabriel**: memoriza quem são as pessoas da família, quais apelidos correspondem a quais números de telefone, quais canais de rádio/música ele ouve pela manhã e quais são seus horários habituais.

---

## 2. Requisitos Funcionais (RF)

- **RF-4.1 (Mapeamento de Parentesco e Apelidos Familiares):**
  - O banco de dados local (`Room / SQLite` criptografado em disco) deve armazenar uma tabela de relacionamentos:
    - `"filho"` $\rightarrow$ Gabriel
    - `"esposa"` / `"filha"` / `"irmão"` $\rightarrow$ Contatos correspondentes
    - Apelidos fonéticos (para o STT nunca errar a grafia de nomes próprios comuns).
  - O agente aprende ativamente: se o pai disser *"Liga pro meu filho"*, o assistente pergunta uma única vez: *"Quem é o seu filho?"*. Ao ouvir *"É o Gabriel"*, registra a associação permanentemente.
- **RF-4.2 (Histórico de Preferências e Favoritos Rápidos):**
  - Armazena as últimas 5 buscas do YouTube que o usuário ouviu por mais de 10 minutos.
  - Ao comando de manhã *"Coloca minhas músicas"* ou *"Coloca meu programa"*, o agente reproduz direto a playlist ou estação favorita sem precisar de nova busca demorada.
- **RF-4.3 (Lembretes Falados e Saudação Humanizada por Horário):**
  - Configuração de rotina matinal opcional:
    - Ao primeiro desbloqueio do celular no dia (ou ao primeiro toque longo no botão de volume após as 07:00):
    - O agente cumprimenta com calor humano: *"Bom dia, seu [Nome]. Hoje é segunda-feira, 9 de novembro. Você tem 3 mensagens novas no WhatsApp. Deseja ouvir agora?"*.
- **RF-4.4 (Privacidade Absoluta e Armazenamento 100% Local):**
  - Todos os dados de rotina, apelidos, contatos frequentes e histórico de comandos ficam gravados **exclusivamente na memória interna do aparelho**. Nenhum dado pessoal do usuário é enviado para servidores externos.

---

## 3. Critérios de Aceite (Definition of Done)

- [ ] **CA-4.1:** Após ensinar uma vez que "filho" é "Gabriel", o comando "Liga pro meu filho" disca com sucesso para o Gabriel.
- [ ] **CA-4.2:** O histórico local armazena os termos mais frequentes do YouTube e os disponibiliza para reprodução rápida.
- [ ] **CA-4.3:** O banco de dados de rotina opera 100% offline sem depender de sincronização externa em nuvem.
