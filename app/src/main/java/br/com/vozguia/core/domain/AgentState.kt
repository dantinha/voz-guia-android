package br.com.vozguia.core.domain

/**
 * Máquina de estados determinística do Voz Guia (UX State Machine).
 */
enum class AgentState {
    /**
     * Serviço ativo em background aguardando comandos físicos ou eventos do sistema.
     */
    IDLE,

    /**
     * Microfone aberto gravando áudio do usuário (STT ativo).
     */
    LISTENING,

    /**
     * Processamento da fala e raciocínio de intenção.
     */
    THINKING,

    /**
     * Resposta falada sendo reproduzida pelo sintetizador (TTS).
     */
    SPEAKING,

    /**
     * Aguardando confirmação vocal explícita de 2 passos do usuário (ex: "Posso enviar?").
     */
    AWAITING_CONFIRMATION,

    /**
     * Despachando cliques, toques ou intents em aplicativos externos (WhatsApp, Telefone, YouTube).
     */
    EXECUTING,

    /**
     * Alerta urgente de entrada (chamada recebida ou mensagem de familiar prioritário).
     */
    INCOMING_ALERT
}
