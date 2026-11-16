package br.com.vozguia.core.feedback

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Controlador de pulso periódico (Heartbeat) de processamento.
 * Garante o princípio Zero Silêncio no Escuro: se uma resposta demorar mais de 1s,
 * o usuário sente um pulso suave de relógio a cada 1000ms até a resposta chegar.
 */
class HeartbeatController(
    private val coroutineScope: CoroutineScope,
    private val intervalMs: Long = 1000L,
    private val initialDelayMs: Long = 1000L,
    private val onHeartbeat: () -> Unit
) {
    private var heartbeatJob: Job? = null

    fun start() {
        if (heartbeatJob?.isActive == true) return

        heartbeatJob = coroutineScope.launch {
            delay(initialDelayMs)
            while (isActive) {
                onHeartbeat()
                delay(intervalMs)
            }
        }
    }

    fun stop() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }

    val isRunning: Boolean get() = heartbeatJob?.isActive == true
}
