package br.com.vozguia.core.accessibility.trigger

/**
 * Ações resultantes da interceptação física de botões de volume.
 */
sealed interface TriggerAction {
    /**
     * Aciona o estado de escuta do assistente (microfone aberto + haptics < 50ms).
     */
    data object ActivateListening : TriggerAction

    /**
     * Interrupção de emergência (silencia fala imediatamente, cancela fluxos pendentes e volta a IDLE).
     */
    data object PanicCancel : TriggerAction

    /**
     * Ajusta o volume do sistema para baixo (-1 step).
     */
    data object AdjustVolumeDown : TriggerAction

    /**
     * Ajusta o volume do sistema para cima (+1 step).
     */
    data object AdjustVolumeUp : TriggerAction

    /**
     * Consumido internamente sem disparar ação externa (ex: durante contagem de hold).
     */
    data object ConsumeQuietly : TriggerAction

    /**
     * Evento não relevante para o assistente; repassado ao sistema Android.
     */
    data object PassThrough : TriggerAction
}
