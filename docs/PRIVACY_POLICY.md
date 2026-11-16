# Política de Privacidade — Voz Guia (Agente Assistivo Android)

**Última atualização:** 2026-11-11  
**Aplicativo:** Voz Guia (`br.com.gabriel.vozguia`)  
**Desenvolvedor / Responsável:** `[SEU NOME COMPLETO]`  
**Contato de suporte e privacidade:** `[SEU_EMAIL_DE_SUPORTE@DOMINIO.COM]`

---

## 1. Propósito do Aplicativo

O **Voz Guia** é uma ferramenta assistiva de acessibilidade (`android:isAccessibilityTool="true"`) projetada para auxiliar pessoas com deficiência visual ou baixa visão a interagir com o smartphone Android por meio de comandos de voz, respostas em áudio/vibração e configurações gerenciadas por um familiar/cuidador.

---

## 2. Divulgação Proeminente de Uso de APIs Sensíveis

### 2.1. API `AccessibilityService` (`BIND_ACCESSIBILITY_SERVICE`)
O Voz Guia utiliza a API de Acessibilidade do Android **estritamente** para:
- Ler o conteúdo textual visível da tela e descrever ações disponíveis ao usuário com deficiência visual;
- Capturar o pressionamento da tecla física **Volume+** (`flagRequestFilterKeyEvents`) para ativação rápida do comando de voz sem necessidade de enxergar a tela;
- Executar gestos e ações de navegação solicitados explicitamente por voz pelo usuário (por exemplo: iniciar uma ligação, abrir vídeos no YouTube ou auxiliar na resposta do WhatsApp).

**Limitações e Proteções:**
- O Voz Guia **não** realiza alterações autônomas em aplicativos bancários ou de pagamento. Quando um app financeiro ou campo sensível (senha, PIN, código OTP, CPF, cartão/CVV) é detectado, o `FinancialShieldGuardrail` e o `SensitiveNodeFilter` interrompem ou ocultam a leitura automaticamente.
- O serviço de acessibilidade **não** é usado para gravação oculta de chamadas, coleta de credenciais ou alteração de configurações do sistema sem consentimento explícito do usuário/cuidador.

### 2.2. Acesso às Notificações (`BIND_NOTIFICATION_LISTENER_SERVICE`)
Utilizado exclusivamente para interceptar notificações recebidas do **WhatsApp** (`com.whatsapp`) para que o usuário possa ouvir quem enviou a mensagem e respondê-la por voz. Notificações de outros aplicativos são ignoradas pelo leitor de WhatsApp.

### 2.3. Outras Permissões Declaradas
- **Microfone (`RECORD_AUDIO`):** Reconhecimento de fala local/sistema para comandos de voz do usuário.
- **Telefone (`CALL_PHONE`, `READ_PHONE_STATE`) e Contatos (`READ_CONTACTS`):** Discagem direta por comando de voz para contatos confiáveis cadastrados pelo cuidador.
- **Sobreposição de tela (`SYSTEM_ALERT_WINDOW`) e Ignorar otimização de bateria (`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`):** Exibição de feedback visual/overlay assistivo e prevenção contra encerramento em segundo plano pelo sistema.
- **Rede (`INTERNET`, `ACCESS_NETWORK_STATE`):** Prevista para validação de conectividade e eventual modo assistido com limite estrito de cota; na versão atual local-first, os fluxos principais de acessibilidade, voz e rotina operam localmente no dispositivo.

---

## 3. Armazenamento, Segurança e Compartilhamento de Dados

- **Processamento Local:** Contatos confiáveis, apelidos fonéticos, lembretes de remédios, favoritos de mídia e histórico de rotina são armazenados **exclusivamente no dispositivo** usando banco de dados local (`Room` SQLite) e contadores de cota protegidos por criptografia (`EncryptedSharedPreferences` com esquema AES-256 via Jetpack Security).
- **Backup em Nuvem Desativado:** O manifesto declara `android:allowBackup="false"` e regras de extração restritas (`data_extraction_rules.xml`), impedindo cópia automática automática dos dados locais para backups externos.
- **Zero Venda ou Rastreamento:** O aplicativo **não** contém SDKs de anúncios, rastreadores de marketing ou telemetria comercial. Dados obtidos via `AccessibilityService` ou notificações **não** são vendidos, alugados ou compartilhados com terceiros.

---

## 4. Retenção e Exclusão de Dados

Como os dados permanecem armazenados localmente no smartphone do usuário:
- O cuidador pode remover contatos confiáveis, lembretes e favoritos a qualquer momento diretamente nas abas **Família** e **Rotina** do aplicativo.
- A exclusão completa e definitiva de todos os dados armazenados ocorre imediatamente ao limpar o armazenamento do aplicativo nas configurações do Android ou ao desinstalar o **Voz Guia**.

---

## 5. Público-Alvo

O Voz Guia não é direcionado a crianças menores de 13 anos e destina-se ao uso por adultos com deficiência visual com apoio de um cuidador/responsável legal.

---

## 6. Alterações nesta Política

Eventuais atualizações futuras (como integração opcional de visão computacional na nuvem com texto previamente anonimizado pelo `DataPrivacySanitizer`) serão refletidas nesta política e divulgadas claramente no aplicativo antes da ativação.
