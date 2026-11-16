package br.com.vozguia.core.accessibility.trigger

import br.com.vozguia.core.domain.AgentState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Interceptor de baixo nível de eventos físicos de teclas de volume.
 * Implementa o gatilho sem mira (Hold Volume Down 900ms) e o botão de pânico (Panic Cancel).
 */
class PhysicalTriggerInterceptor(
    private val coroutineScope: CoroutineScope,
    private val holdThresholdMs: Long = 900L,
    private val tapThresholdMs: Long = 500L
) {
    var onHoldTriggered: ((TriggerAction) -> Unit)? = null

    private var keyDownTimestamp: Long = 0L
    private var holdJob: Job? = null
    private var isHoldTriggered: Boolean = false

    companion object {
        const val KEYCODE_VOLUME_UP = 24
        const val KEYCODE_VOLUME_DOWN = 25
    }

    fun onKeyEvent(
        keyCode: Int,
        isKeyDown: Boolean,
        eventTimeMs: Long,
        currentState: AgentState
    ): TriggerAction {
        when (keyCode) {
            KEYCODE_VOLUME_UP -> {
                return if (isKeyDown) TriggerAction.AdjustVolumeUp else TriggerAction.ConsumeQuietly
            }

            KEYCODE_VOLUME_DOWN -> {
                // Se o agente estiver ativo (falando, ouvindo, pensando), qualquer toque é PanicCancel imediato (< 50ms)
                val isActiveState = currentState == AgentState.SPEAKING ||
                        currentState == AgentState.LISTENING ||
                        currentState == AgentState.THINKING ||
                        currentState == AgentState.AWAITING_CONFIRMATION

                if (isActiveState) {
                    holdJob?.cancel()
                    isHoldTriggered = false
                    return if (isKeyDown) TriggerAction.PanicCancel else TriggerAction.ConsumeQuietly
                }

                // Estado IDLE (repouso)
                if (isKeyDown) {
                    if (keyDownTimestamp != 0L) {
                        // Repetição automática de tecla do Android ignorada
                        return TriggerAction.ConsumeQuietly
                    }
                    keyDownTimestamp = eventTimeMs
                    isHoldTriggered = false

                    holdJob = coroutineScope.launch {
                        delay(holdThresholdMs)
                        isHoldTriggered = true
                        onHoldTriggered?.invoke(TriggerAction.ActivateListening)
                    }

                    return TriggerAction.ConsumeQuietly
                } else {
                    // Botão solto (ACTION_UP)
                    holdJob?.cancel()
                    val pressDuration = eventTimeMs - keyDownTimestamp
                    keyDownTimestamp = 0L

                    if (isHoldTriggered) {
                        isHoldTriggered = false
                        // Hold longo já foi disparado; consome sem alterar o volume do aparelho
                        return TriggerAction.ConsumeQuietly
                    }

                    return if (pressDuration < tapThresholdMs || pressDuration < holdThresholdMs) {
                        TriggerAction.AdjustVolumeDown
                    } else {
                        TriggerAction.ConsumeQuietly
                    }
                }
            }

            else -> return TriggerAction.PassThrough
        }
    }
}
